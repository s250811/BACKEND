package backend.infrastructure.adapter.out.persistence.db.r2dbc.event;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface EventAuditR2dbcRepository extends R2dbcRepository<EventAuditEntity, Long> {
}