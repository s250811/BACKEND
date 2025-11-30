package backend.application.port.out.notification;


import backend.domain.notification.model.Notification;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificationRepositoryPort {
    Mono<Notification> save(Notification notification);
    Mono<Notification> findById(Long id);
    Flux<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, int page, int size);
    Mono<Long> countUnreadByRecipientId(Long recipientId);
}

