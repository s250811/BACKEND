package backend.application.service;

import backend.application.port.in.realtime.RealTimeStreamUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class RealTimeStreamManager implements RealTimeStreamUseCase {

    private final ObjectMapper objectMapper;
    private final Map<Long, Set<WebSocketSession>> workspaceSessions = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> registerSession(Long workspaceId, WebSocketSession session) {
        return Mono.fromRunnable(() -> {
            workspaceSessions.computeIfAbsent(workspaceId, k -> ConcurrentHashMap.newKeySet())
                    .add(session);
            log.info("WebSocket session registered for workspace: {} (total sessions: {})",
                    workspaceId, workspaceSessions.get(workspaceId).size());
        });
    }

    @Override
    public void unregisterSession(Long workspaceId, WebSocketSession session) {
        Set<WebSocketSession> sessions = workspaceSessions.get(workspaceId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                workspaceSessions.remove(workspaceId);
                log.info("All sessions removed for workspace: {}", workspaceId);
            } else {
                log.debug("Session removed from workspace: {} (remaining: {})", workspaceId, sessions.size());
            }
        }
    }

    @Override
    public Mono<Void> broadcastToWorkspace(Long workspaceId, Object event) {
        Set<WebSocketSession> sessions = workspaceSessions.get(workspaceId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("No active sessions for workspace {} - ignoring event", workspaceId);
            return Mono.empty();
        }

        try {
            String payload = objectMapper.writeValueAsString(event);

            // 세션들을 복사하여 안전하게 순회
            Set<WebSocketSession> sessionsCopy = new HashSet<>(sessions);

            return Flux.fromIterable(sessionsCopy)
                    .filter(WebSocketSession::isOpen)
                    .flatMap(session ->
                            session.send(Mono.just(session.textMessage(payload)))
                                    .doOnError(error -> {
                                        log.error("Failed to send message to session in workspace {}", workspaceId, error);
                                        unregisterSession(workspaceId, session);
                                    })
                                    .onErrorResume(error -> Mono.empty()) // 개별 세션 오류 무시
                    )
                    .doOnComplete(() ->
                            log.debug("Event published to {} sessions in workspace {}", sessionsCopy.size(), workspaceId)
                    )
                    .then();

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event for workspace {}", workspaceId, e);
            return Mono.error(e);
        }
    }

    @Override
    public void cleanupInactiveSessions() {
        workspaceSessions.entrySet().removeIf(entry -> {
            Long workspaceId = entry.getKey();
            Set<WebSocketSession> sessions = entry.getValue();

            // 닫힌 세션들 제거
            sessions.removeIf(session -> !session.isOpen());

            boolean isEmpty = sessions.isEmpty();
            if (isEmpty) {
                log.info("Cleaning up empty workspace sessions: {}", workspaceId);
            }
            return isEmpty;
        });
    }
}