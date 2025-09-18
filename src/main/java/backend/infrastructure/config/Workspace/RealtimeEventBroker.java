package backend.infrastructure.config.Workspace;

import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

public interface RealtimeEventBroker {
    Mono<Void> registerSession(Long workspaceId, WebSocketSession session);
    void unregisterSession(Long workspaceId, WebSocketSession session);
    Mono<Void> publishEvent(Long workspaceId, Object event);
    void cleanupInactiveSessions();
}
