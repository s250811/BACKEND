package backend.infrastructure.adapter.in.messaging.kafka.notification;

import backend.application.port.in.notification.NotificationUseCase;
import backend.domain.event.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;


@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaNotificationConsumer {
    private final NotificationUseCase notificationUseCase;
    private final ObjectMapper objectMapper;

    /**
     * Spring Cloud Stream Functional Consumer
     * - notificationConsumer 함수는 자동으로 notificationConsumer-in-0' binding과 연결된다.
     * - 해당 binding의 destination(notification-events)을 구독한다.
     * - 예외 발생 시 max-attempts 설정에 따라 재시도되며, 재시도 실패 시 DLQ(notification-events.dlq)로 전달된다.
     * - offset commit은 binder 설정(enable.auto.commit=false)에 따라 처리 성공 시점에 관리된다.
     */
    @Bean
    public Function<Flux<Message<String>>, Mono<Void>> notificationConsumer() {
        return flux -> flux
                .flatMap(message -> {
                    Event event;
                    try {
                        event = objectMapper.readValue(message.getPayload(), Event.class);
                    } catch (Exception e) {
                        return Mono.error(e);
                    }

                    return notificationUseCase.processEvent(event)
                            .doOnSuccess(v -> log.info("Notification 이벤트 처리 성공: eventId={}", event.getId()))
                            .doOnError(err -> log.error("Notification 이벤트 처리 실패: eventId={}", event.getId(), err));
                })
                .then();
    }
}