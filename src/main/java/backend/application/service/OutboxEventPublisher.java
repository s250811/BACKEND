package backend.application.service;

import backend.application.port.out.event.audit.EventAuditRepositoryPort;
import backend.application.port.out.messaging.EventProducerPort;
import backend.domain.event.Event;
import backend.domain.event.audit.EventAudit;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {
    private final EventAuditRepositoryPort eventAuditRepository;
    private final EventProducerPort eventProducer;
    private final ObjectMapper objectMapper;

    @Scheduled(initialDelay = 60000, fixedDelay = 10000)
    public void publishPendingEvents() {
        eventAuditRepository.findPendingEvents()
                .flatMap(this::lockForProcessing)
                .flatMap(this::publishAndUpdateStatus)
                .subscribe();
    }

    private Mono<EventAudit> lockForProcessing(EventAudit eventAudit) {
        eventAudit.markProcessing();
        return eventAuditRepository.save(eventAudit);
    }

    private Mono<EventAudit> publishAndUpdateStatus(EventAudit eventAudit) {
        return Mono.fromCallable(() -> objectMapper.readValue(eventAudit.getPayload(), Event.class))
                .flatMap(event -> eventProducer.publishEvent(event)
                        .then(Mono.defer(() -> {
                            eventAudit.markSuccess();
                            return eventAuditRepository.save(eventAudit);
                        }))
                )
                .onErrorResume(ex -> {
                    log.error("이벤트 발행 실패", ex);
                    eventAudit.markFailed(((Throwable) ex).getMessage());
                    return eventAuditRepository.save(eventAudit);
                });
    }
}
