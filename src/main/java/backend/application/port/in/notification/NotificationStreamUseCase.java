package backend.application.port.in.notification;

import backend.domain.notification.model.Notification;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificationStreamUseCase {
    Flux<Notification> createConnection(Long userId);
    void removeConnection(Long userId);
    int getActiveConnectionCount();
    boolean isConnected(Long userId);
    Mono<Void> sendToUser(Long userId, Notification notification);
}
