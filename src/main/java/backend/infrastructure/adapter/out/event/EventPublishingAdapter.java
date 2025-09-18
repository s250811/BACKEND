package backend.infrastructure.adapter.out.event;

import backend.application.port.out.event.KafkaEventPublishPort;
import backend.domain.event.Event;
import backend.exception.workspace.WorkspaceErrorCode;
import backend.exception.workspace.WorkspaceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublishingAdapter implements KafkaEventPublishPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public <T extends Serializable> Mono<Void> publishEvent(Event<T> event) {
        String partitionKey = event.getPartitionKey(); // 각 엔티티 순서 보장하기 위한 key

        List<String> topics = event.getTopics();

        if (topics == null || topics.isEmpty()) {
            throw new WorkspaceException(WorkspaceErrorCode.EVENT_TOPIC_ERROR);
        }

        return Flux.fromIterable(topics)
                .flatMap(topic ->
                                Mono.fromFuture(() -> kafkaTemplate.send(topic, partitionKey, event))
                                        .doOnSuccess(result -> log.debug(
                                                "이벤트 발행 성공: topic={}, partition={}, offset={}, eventId={}",
                                                topic,
                                                result.getRecordMetadata().partition(),
                                                result.getRecordMetadata().offset(),
                                                event.getId()
                                        ))
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
                        , 4) // 최대 4개 병렬
                .then();
        }
}