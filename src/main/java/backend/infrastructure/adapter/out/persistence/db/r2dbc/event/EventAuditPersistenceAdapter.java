package backend.infrastructure.adapter.out.persistence.db.r2dbc.event;

import backend.application.port.out.event.audit.EventAuditRepositoryPort;
import backend.domain.event.EventId;
import backend.domain.event.EventType;
import backend.domain.event.audit.EventAudit;
import backend.domain.event.audit.EventAuditId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class EventAuditPersistenceAdapter implements EventAuditRepositoryPort {

    private final EventAuditR2dbcRepository repository;

    @Override
    public Mono<EventAudit> save(EventAudit eventAudit) {
        EventAuditEntity entity = toEntity(eventAudit);
        return repository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Mono<EventAudit> findById(Long id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    private EventAuditEntity toEntity(EventAudit eventAudit) {
        return EventAuditEntity.builder()
                .id(eventAudit.getIdValue())
                .eventId(eventAudit.getEventId().getValue())
                .eventType(eventAudit.getEventType().name())
                .status(eventAudit.getStatus())
                .errorMessage(eventAudit.getErrorMessage())
                .updatedAt(eventAudit.getUpdatedAt())
                .createdAt(eventAudit.getCreatedAt())
                .build();
    }

    private EventAudit toDomain(EventAuditEntity entity) {
        return EventAudit.builder()
                .id(EventAuditId.of(entity.getId()))
                .eventId(EventId.of(entity.getEventId()))
                .eventType(EventType.valueOf(entity.getEventType()))
                .status(entity.getStatus())
                .errorMessage(entity.getErrorMessage())
                .updatedAt(entity.getUpdatedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}