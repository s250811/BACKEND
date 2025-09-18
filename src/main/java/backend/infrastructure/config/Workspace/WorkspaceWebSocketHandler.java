package backend.infrastructure.config.Workspace;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class WorkspaceWebSocketHandler implements WebSocketHandler {

    private final RealtimeEventBroker eventBroker;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // URL에서 workspaceId 추출
        Long workspaceId = extractWorkspaceId(session);
        if (workspaceId == null) {
            log.error("Invalid workspace ID in WebSocket URL: {}", session.getHandshakeInfo().getUri());
            return session.close(CloseStatus.BAD_DATA);
        }

        log.info("WebSocket connection established for workspace: {}", workspaceId);

        // 세션 등록
        return eventBroker.registerSession(workspaceId, session)
                .then(
                        // 연결 유지 및 메시지 수신 처리
                        session.receive()
                                .doOnNext(message -> handleIncomingMessage(workspaceId, session, message))
                                .doOnComplete(() -> {
                                    log.info("WebSocket connection closed for workspace: {}", workspaceId);
                                    eventBroker.unregisterSession(workspaceId, session);
                                })
                                .doOnError(error -> {
                                    log.error("WebSocket error for workspace: {}", workspaceId, error);
                                    eventBroker.unregisterSession(workspaceId, session);
                                })
                                .then()
                );
    }

    private Long extractWorkspaceId(WebSocketSession session) {
        try {
            String path = session.getHandshakeInfo().getUri().getPath();
            // "/ws/workspace/{workspaceId}" 패턴에서 workspaceId 추출
            String[] segments = path.split("/");
            if (segments.length >= 4 && "ws".equals(segments[1]) && "workspace".equals(segments[2])) {
                return Long.parseLong(segments[3]);
            }
        } catch (Exception e) {
            log.error("Failed to extract workspace ID from WebSocket URL", e);
        }
        return null;
    }


    private void handleIncomingMessage(Long workspaceId, WebSocketSession session, WebSocketMessage message) {
        if (message.getType() == WebSocketMessage.Type.TEXT) {
            String payload = message.getPayloadAsText();
            log.debug("Received WebSocket message from workspace {}: {}", workspaceId, payload);

            // 필요한 경우 클라이언트로부터의 메시지 처리 로직 구현
            // 예: 핑/퐁, 인증, 특별한 명령어 등
            if ("ping".equals(payload.trim())) {
                session.send(Mono.just(session.textMessage("pong")))
                        .doOnError(error -> log.error("Failed to send pong response", error))
                        .subscribe();
            }
        }
    }
}
