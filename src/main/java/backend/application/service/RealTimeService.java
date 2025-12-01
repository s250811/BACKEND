package backend.application.service;

import backend.application.port.in.realtime.RealTimeStreamUseCase;
import backend.application.port.in.realtime.RealTimeUseCase;
import backend.domain.event.Event;
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
    private final RealTimeStreamUseCase realTimeStreamUseCase;

    @Override
    public Mono<Void> processEvent(Event<?> event, Long workspaceId) {
        Map<String, Object> wsPayload = createWebSocketPayload(event, workspaceId);

        return realTimeStreamUseCase.broadcastToWorkspace(workspaceId, wsPayload);
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