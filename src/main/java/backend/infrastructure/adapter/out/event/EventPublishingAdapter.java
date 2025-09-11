package backend.infrastructure.adapter.out.event;

import backend.application.port.out.event.EventPublishingPort;
import backend.domain.event.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublishingAdapter implements EventPublishingPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String NOTIFICATION_TOPIC = "notification-events";

    @Override
    public <T extends Serializable> Mono<Void> publishEvent(Event<T> event) {
        String key = event.getId().getValue().toString();
        return Mono.fromFuture(() -> {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(NOTIFICATION_TOPIC, key, event);

            return future.handle((result, throwable) -> {
                if (throwable != null) {
                    log.error("이벤트 발행 실패: eventType={}, eventId={}",
                            event.getType(), event.getId(), throwable);
                    throw new RuntimeException("이벤트 발행 실패", throwable);
                } else {
                    log.debug("이벤트 발행 성공: partition={}, offset={}, eventId={}",
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            event.getId());
                    return result;
                }
            });
        }).then();
    }
}