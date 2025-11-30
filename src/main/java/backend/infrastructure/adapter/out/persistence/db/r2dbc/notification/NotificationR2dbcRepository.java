package backend.infrastructure.adapter.out.persistence.db.r2dbc.notification;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificationR2dbcRepository extends R2dbcRepository<NotificationEntity, Long> {
    Flux<NotificationEntity> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);
    Mono<Long> countByRecipientIdAndIsReadFalse(Long recipientId);
}