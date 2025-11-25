package backend.infrastructure.adapter.out.persistence.db.r2dbc.notification;

import backend.application.port.out.notification.NotificationRepositoryPort;
import backend.domain.event.EventId;
import backend.domain.notification.model.Notification;
import backend.domain.notification.model.NotificationId;
import backend.domain.user.model.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class NotificationPersistenceAdapter implements NotificationRepositoryPort {

    private final NotificationR2dbcRepository repository;

    @Override
    public Mono<Notification> save(Notification notification) {
        NotificationEntity entity = toEntity(notification);
        return repository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Mono<Notification> findById(Long id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Flux<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable)
                .map(this::toDomain);
    }

    @Override
    public Mono<Long> countUnreadByRecipientId(Long recipientId) {
        return repository.countByRecipientIdAndIsReadFalse(recipientId);
    }

    private NotificationEntity toEntity(Notification notification) {
        return NotificationEntity.builder()
                .id(notification.getIdValue())
                .recipientId(notification.getRecipientId().getValue())
                .senderId(notification.getSenderId().getValue())
                .isRead(notification.getIsRead())
                .eventId(notification.getEventId().getValue())
                .type(notification.getType())
                .readAt(notification.getReadAt())
                .message(notification.getMessage())
                .build();
    }


    private Notification toDomain(NotificationEntity entity) {
        return new Notification(
                NotificationId.of(entity.getId()),
                UserId.of(entity.getSenderId()),
                UserId.of(entity.getRecipientId()),
                entity.getIsRead(),
                EventId.of(entity.getEventId()),
                entity.getType(),
                entity.getCreatedAt(),
                entity.getReadAt(),
                entity.getMessage()
        );
    }
}