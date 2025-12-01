package backend.infrastructure.adapter.out.persistence.db.r2dbc.event;

import backend.domain.event.audit.EventProcessingStatus;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface EventAuditR2dbcRepository extends R2dbcRepository<EventAuditEntity, Long> {
    Flux<EventAuditEntity> findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(EventProcessingStatus status, int retryCount);
}