package backend.infrastructure.adapter.out.event;

import backend.application.port.out.event.EventPublishingPort;
import backend.domain.event.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.Serializable;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublishingAdapter implements EventPublishingPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String NOTIFICATION_TOPIC = "notification-events";

    @Override
    public <T extends Serializable> Mono<Void> publishEvent(Event<T> event) {
        String partitionKey = event.getPartitionKey(); // 각 엔티티 순서 보장하기 위한 key
        return Mono.fromFuture(() -> kafkaTemplate.send(NOTIFICATION_TOPIC, partitionKey, event))
                .doOnSuccess(result -> log.debug("이벤트 발행 성공: partition={}, offset={}, eventId={}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.getId()))
                .onErrorResume(throwable -> {
                    // Kafka Producer의 모든 retry가 실패한 후에만 실행됨
                    if (throwable instanceof org.apache.kafka.common.errors.TimeoutException ||
                            throwable instanceof org.apache.kafka.common.errors.RetriableException) {
                        log.error("이벤트 발행 최종 실패 (모든 retry 소진): eventType={}, eventId={}",
                                event.getType(), event.getId(), throwable);
                        // 비즈니스 로직에 영향을 주지 않도록 에러 무시
                        return Mono.empty();
                    }
                    // 다른 에러는 전파
                    return Mono.error(throwable);
                })
                .then();
    }
}