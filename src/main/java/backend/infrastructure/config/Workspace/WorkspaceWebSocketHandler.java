package backend.infrastructure.config.Workspace;

import backend.application.port.out.auth.TokenServicePort;
import backend.application.port.out.user.UserRepositoryPort;
import backend.domain.user.model.UserId;
import backend.infrastructure.adapter.out.auth.JwtTokenAdapter;
import backend.infrastructure.security.JwtAuthenticationManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.*;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class WorkspaceWebSocketHandler implements WebSocketHandler {

    private final RealtimeEventBroker eventBroker;
    private final TokenServicePort jwtTokenAdapter;
    private final UserRepositoryPort userRepository;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Long workspaceId = extractWorkspaceId(session);
        if (workspaceId == null) {
            log.error("Invalid workspace ID in WebSocket URL: {}", session.getHandshakeInfo().getUri());
            return session.close(CloseStatus.BAD_DATA.withReason("Invalid workspace ID"));
        }

        log.info("WebSocket connection attempt for workspace: {}", workspaceId);

        // 1) 토큰 추출 및 기본 검증 (다양한 방법으로 토큰 추출 시도)
        return extractTokenOrClose(session, workspaceId)
                // 2) 토큰으로 SecurityContext 생성 (DB 조회 포함)
                .flatMap(token -> buildSecurityContextFromToken(token, session, workspaceId))
                // 3) 세션 등록 및 메시지 수신을 동일한 SecurityContext로 실행
                .flatMap(securityContext ->
                        eventBroker.registerSession(workspaceId, session)
                                .then(runReceiveChain(session, workspaceId))
                                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
                )
                // 4) 인증/등록/처리 중 에러 발생 시 세션 닫음
                .onErrorResume(err -> {
                    log.warn("WebSocket auth/register error for workspace {}: {}", workspaceId, err.toString());
                    return closeUnauthorized(session);
                });
    }

    private Mono<String> extractTokenOrClose(WebSocketSession session, Long workspaceId) {
        HandshakeInfo handshakeInfo = session.getHandshakeInfo();

        // Authorization 헤더에서 토큰 추출
        String token = extractTokenFromAuthHeader(handshakeInfo.getHeaders());

        log.info("Authorization header extraction for workspace {}: found={}", workspaceId, token != null);
        if (token != null) {
            log.debug("Token extracted from Authorization header for workspace {}", workspaceId);
        }

        if (token == null) {
            log.warn("WebSocket auth failed (no Authorization header or invalid format) for workspace {}", workspaceId);
            return closeUnauthorized(session).then(Mono.error(new RuntimeException("No Authorization header with Bearer token")));
        }

        // 토큰 검증
        try {
            if (!jwtTokenAdapter.validateToken(token)) {
                log.warn("WebSocket auth failed (invalid token) for workspace {}", workspaceId);
                return closeUnauthorized(session).then(Mono.error(new RuntimeException("Invalid token")));
            }
            log.info("Token validation successful for workspace {}", workspaceId);
        } catch (Exception e) {
            log.error("Token validation error for workspace {}: {}", workspaceId, e.getMessage());
            return closeUnauthorized(session).then(Mono.error(e));
        }

        return Mono.just(token);
    }

    private Mono<SecurityContextImpl> buildSecurityContextFromToken(String token, WebSocketSession session, Long workspaceId) {
        Long userId;
        try {
            userId = jwtTokenAdapter.getUserIdAsLongFromToken(token);
            log.info("Extracted userId {} from token for workspace {}", userId, workspaceId);
        } catch (Exception e) {
            log.warn("Failed to parse userId from token for workspace {}: {}", workspaceId, e.toString());
            return Mono.error(e);
        }

        return userRepository.findById(UserId.of(userId))
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("WebSocket auth failed (user not found) for userId {} in workspace {}", userId, workspaceId);
                    return Mono.error(new RuntimeException("User not found"));
                }))
                .map(user -> {
                    log.info("User {} authenticated successfully for workspace {}", user.getEmail(), workspaceId);

                    JwtAuthenticationManager.AuthenticatedUser principal =
                            new JwtAuthenticationManager.AuthenticatedUser(user.getIdValue(), user.getEmail());

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            principal,
                            token,
                            Collections.singleton(new SimpleGrantedAuthority("MEMBER"))
                    );

                    return new SecurityContextImpl(auth);
                });
    }

    private Mono<Void> runReceiveChain(WebSocketSession session, Long workspaceId) {
        return session.receive()
                .doOnNext(message -> {
                    try {
                        handleIncomingMessage(workspaceId, session, message);
                    } catch (Exception ex) {
                        log.error("Error handling incoming message for workspace {}: {}", workspaceId, ex.toString());
                    }
                })
                .doOnError(error -> {
                    log.error("WebSocket error for workspace {}: {}", workspaceId, error.toString());
                    eventBroker.unregisterSession(workspaceId, session);
                })
                .doOnComplete(() -> {
                    log.info("WebSocket connection closed for workspace {}", workspaceId);
                    eventBroker.unregisterSession(workspaceId, session);
                })
                .then();
    }

    private Mono<Void> closeUnauthorized(WebSocketSession session) {
        return session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized"))
                .onErrorResume(err -> {
                    log.error("Failed to close WebSocket session after auth failure: {}", err.toString());
                    return Mono.empty();
                });
    }

    // workspaceId 추출 메서드
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

    // Authorization 헤더에서 토큰 추출
    private String extractTokenFromAuthHeader(HttpHeaders headers) {
        try {
            List<String> authHeaders = headers.get(HttpHeaders.AUTHORIZATION);
            if (authHeaders != null && !authHeaders.isEmpty()) {
                String authHeader = authHeaders.get(0);
                log.debug("Authorization header found: {}", authHeader != null ? "Bearer ***" : "null");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    log.debug("Token extracted from Authorization header, length: {}", token.length());
                    return token;
                }
            } else {
                log.debug("No Authorization header found in WebSocket handshake");
            }
        } catch (Exception e) {
            log.warn("Failed to extract token from Authorization header: {}", e.getMessage());
        }
        return null;
    }

    private void handleIncomingMessage(Long workspaceId, WebSocketSession session, WebSocketMessage message) {
        if (message.getType() == WebSocketMessage.Type.TEXT) {
            String payload = message.getPayloadAsText();
            log.debug("Received WebSocket message from workspace {}: {}", workspaceId, payload);

            // 핑/퐁 처리
            if ("ping".equals(payload.trim())) {
                session.send(Mono.just(session.textMessage("pong")))
                        .doOnError(error -> log.error("Failed to send pong response", error))
                        .subscribe();
            }
            // 다른 메시지 처리 로직을 여기에 추가
        }
    }
}
