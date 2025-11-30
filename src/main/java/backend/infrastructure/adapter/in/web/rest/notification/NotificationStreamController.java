package backend.infrastructure.adapter.in.web.rest.notification;

import backend.application.port.in.notification.NotificationStreamUseCase;
import backend.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationStreamController {

    private final NotificationStreamUseCase notificationStreamUseCase;

    // 10초마다 ping 전송으로 연결 유지
    private final Flux<ServerSentEvent<String>> ping =
            Flux.interval(Duration.ofSeconds(10))
                    .map(tick -> ServerSentEvent.<String>builder()
                            .comment("ping") // comment로 전송하여 클라이언트에서 무시 가능
                            .build());

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "실시간 알림 스트림 연결")
    public Flux<ServerSentEvent<? extends Serializable>> streamNotifications() {
        return SecurityUtils.getCurrentUserId()
                .flatMapMany(userId -> {
                    log.info("SSE 스트림 연결 요청 - 사용자 ID: {}", userId);

                    return Flux.merge(
                                    notificationStreamUseCase.createConnection(userId)
                                            .map(notification ->
                                                    ServerSentEvent.builder(notification)
                                                            .id(notification.getIdValue().toString())
                                                            .event("notification")
                                                            .build()
                                            ),
                                    ping
                            )
                            .doOnCancel(() -> {
                                log.info("클라이언트 연결 취소 - 사용자 ID: {}", userId);
                                notificationStreamUseCase.removeConnection(userId);
                            })
                            .doOnComplete(() -> {
                                log.info("SSE 스트림 완료 - 사용자 ID: {}", userId);
                                notificationStreamUseCase.removeConnection(userId);
                            })
                            .doOnError(error -> {
                                log.error("SSE 스트림 에러 - 사용자 ID: {}, 에러: {}", userId, error.getMessage());
                                notificationStreamUseCase.removeConnection(userId);
                            });
                });
    }

    @GetMapping("/connection-status")
    @Operation(summary = "현재 SSE 연결 상태 조회")
    public Mono<ConnectionStatusResponse> getConnectionStatus() {
        return SecurityUtils.getCurrentUserId()
                .map(userId -> new ConnectionStatusResponse(
                        notificationStreamUseCase.isConnected(userId),
                        notificationStreamUseCase.getActiveConnectionCount()
                ))
                .defaultIfEmpty(new ConnectionStatusResponse(false, 0));
    }

    public record ConnectionStatusResponse(
            boolean connected,
            int totalConnections
    ) {}
}