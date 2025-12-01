package backend.application.port.out.event.audit;

import backend.domain.event.audit.EventAudit;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventAuditRepositoryPort {
    Mono<EventAudit> save(EventAudit eventAudit);
    Mono<EventAudit> findById(Long id);
    Flux<EventAudit> findPendingEvents();}