package backend.application.port.in.notification;

import backend.domain.event.Event;
import backend.domain.notification.model.Notification;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;

public interface NotificationUseCase {
    <T extends Serializable> Mono<Void> processEvent(Event<T> event);
    Mono<Flux<Notification>> getNotificationsByRecipientId(Long recipientId, int page, int size);
     Mono<Void> markAsRead(Long notificationId);
     Mono<Long> getUnreadCount(Long userId);
}
