package backend.application.port.in.realtime;

import backend.domain.event.Event;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

public interface RealTimeStreamUseCase {
    Mono<Void> registerSession(Long workspaceId, WebSocketSession session);
    void unregisterSession(Long workspaceId, WebSocketSession session);
    Mono<Void> broadcastToWorkspace(Long workspaceId, Object event);
    void cleanupInactiveSessions();
}
