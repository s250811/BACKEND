package backend.application.service;

import backend.application.port.in.realtime.RealTimeStreamUseCase;
import backend.application.port.in.realtime.RealTimeUseCase;
import backend.application.port.out.event.audit.EventAuditRepositoryPort;
import backend.domain.event.Event;
import backend.domain.event.EventId;
import backend.domain.event.EventType;
import backend.domain.event.audit.EventAudit;
import backend.domain.event.audit.EventProcessingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class RealTimeService implements RealTimeUseCase {

    private final EventAuditRepositoryPort eventAuditRepository;
    private final RealTimeStreamUseCase realTimeStreamUseCase;

    @Override
    public Mono<Void> processEvent(Event<?> event, Long workspaceId) {
        EventId eventId = event.getId();
        EventType eventType = event.getType();

        // 1) 멱등성 체크 + audit 확보
        return eventAuditRepository.findById(eventId.value())
                .flatMap(existing -> {
                    if (existing.getStatus() == EventProcessingStatus.SUCCESS) {
                        log.info("Event {} already processed successfully. Skipping.", eventId.value());
                        return Mono.<EventAudit>empty();
                    }
                    return Mono.just(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    EventAudit started = EventAudit.createStarted(eventId, eventType);
                    return eventAuditRepository.save(started);
                }))
                .flatMap(audit -> {
                    // WebSocket payload 생성
                    Map<String, Object> wsPayload = createWebSocketPayload(event, workspaceId);

                    // 실시간 스트림에 발행
                    return realTimeStreamUseCase.broadcastToWorkspace(workspaceId, wsPayload)
                            .then(Mono.defer(() -> {
                                audit.markSuccess();
                                return eventAuditRepository.save(audit);
                            }))
                            .onErrorResume(err -> {
                                audit.markFailed(err.getMessage());
                                return eventAuditRepository.save(audit)
                                        .then(Mono.error(err));
                            });
                })
                .then();
    }

    private Map<String, Object> createWebSocketPayload(Event<?> event, Long workspaceId) {
        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("eventId", String.valueOf(event.getId().value()));
        wsPayload.put("type", event.getType().name());
        wsPayload.put("workspaceId", workspaceId);
        wsPayload.put("payload", event.getParam());
        wsPayload.put("timestamp", LocalDateTime.now().toString());
        wsPayload.put("source", "kafka");
        return wsPayload;
    }
}