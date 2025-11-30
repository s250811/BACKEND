package backend.infrastructure.adapter.in.messaging.kafka.notification;

import backend.application.port.in.notification.NotificationUseCase;
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
    public class KafkaNotificationConsumer {
        private final NotificationUseCase notificationUseCase;

        @KafkaListener(topics = "${spring.kafka.topics.notification-events}", groupId = "${spring.kafka.consumer.group-id}")
        public void handleNotificationEvent(@Payload Event event, Acknowledgment acknowledgment) {
            notificationUseCase.processEvent(event)
                    .doOnSuccess(unused -> acknowledgment.acknowledge())
                    .doOnError(error -> log.error("이벤트 처리 실패", error))
                    .subscribe();
        }
    }