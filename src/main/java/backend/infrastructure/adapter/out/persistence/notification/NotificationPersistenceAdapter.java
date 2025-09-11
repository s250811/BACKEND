package backend.infrastructure.adapter.out.persistence.notification;

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

import java.io.Serializable;

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

    @Override
    public Mono<Void> deleteByIdAndRecipientId(Long id, Long recipientId) {
        return repository.deleteByIdAndRecipientId(id, recipientId);
    }

    private NotificationEntity toEntity(Notification notification) {
        return NotificationEntity.builder()
                .id(notification.getIdValue())
                .recipientId(notification.getRecipientId().getValue())
                .senderId(notification.getSenderId().getValue())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }


    private Notification toDomain(NotificationEntity entity) {
        return new Notification(
                NotificationId.of(entity.getId()),
                UserId.of(entity.getRecipientId()),
                UserId.of(entity.getSenderId()),
                entity.getIsRead(),
                EventId.of(entity.getEventId()),
                entity.getType(),
                entity.getCreatedAt(),
                entity.getReadAt(),
                entity.getMessage(),
        null
                );
    }
}