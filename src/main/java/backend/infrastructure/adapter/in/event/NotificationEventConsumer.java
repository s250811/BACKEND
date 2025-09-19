package backend.infrastructure.adapter.in.event;

import backend.application.port.in.NotificationUseCase;
import backend.domain.event.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {
    private final NotificationUseCase notificationUseCase;

    @KafkaListener(topics = "notification-events", groupId = "pm-backend")
    public void handleNotificationEvent(@Payload Event event, Acknowledgment acknowledgment) {
        notificationUseCase.processNotificationEvent(event)
                    .doOnSuccess(unused -> acknowledgment.acknowledge())
                    .doOnError(error -> log.error("이벤트 처리 실패", error))
                    .subscribe();
    }
}