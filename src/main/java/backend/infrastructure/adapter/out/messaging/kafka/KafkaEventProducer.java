package backend.infrastructure.adapter.out.messaging.kafka;

import backend.application.port.out.messaging.EventProducerPort;
import backend.domain.common.ValueObject;
import backend.domain.event.Event;
import backend.domain.event.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventProducer implements EventProducerPort {

    @Value("${spring.kafka.topics.real-time-events}")
    private String realTimeEventsTopic;
    @Value("${spring.kafka.topics.notification-events}")
    private String notificationEventsTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public <T extends ValueObject & Serializable> Mono<Void> publishEvent(Event<T> event) {
        T param = event.getParam();
        String partitionKey = String.valueOf(param.getValue()); // 각 엔티티 순서 보장하기 위한 key
        List<String> topics = resolveTargetTopics(event);

        return Flux.fromIterable(topics)
                .flatMap(topic ->
                        Mono.fromFuture(() -> kafkaTemplate.send(topic, partitionKey, event))
                                .doOnSuccess(result -> log.debug("이벤트 발행 성공: topic={}, partition={}, offset={}, eventId={}",
                                        topic,
                                        result.getRecordMetadata().partition(),
                                        result.getRecordMetadata().offset(),
                                        event.getId()))
                                .onErrorResume(throwable -> {
                                    // Kafka Producer의 모든 retry가 실패한 후에만 실행됨
                                    if (throwable instanceof org.apache.kafka.common.errors.TimeoutException ||
                                            throwable instanceof org.apache.kafka.common.errors.RetriableException) {
                                        log.error("이벤트 발행 최종 실패 (모든 retry 소진): topic={}, eventType={}, eventId={}",
                                                topic, event.getType(), event.getId(), throwable);
                                        // 비즈니스 로직에 영향을 주지 않도록 에러 무시
                                        return Mono.empty();
                                    }
                                    // 다른 에러는 전파
                                    return Mono.error(throwable);
                                })
                )
                .then();
    }

    private List<String> resolveTargetTopics(Event<?> event) {
        List<String> topics = new ArrayList<>();

        topics.add(realTimeEventsTopic);

        EventType type = event.getType();
        if (type == EventType.TASK_UPDATED || type == EventType.COMMENT_UPDATED) {
            topics.add(notificationEventsTopic);
        }

        return topics;
    }

}