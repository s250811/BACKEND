package backend.infrastructure.adapter.in.web;

import backend.application.service.SseNotificationService;
import backend.infrastructure.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class SseController {

    private final SseNotificationService sseNotificationService;
    private final ObjectMapper objectMapper;

    // 10초마다 ping 전송으로 연결 유지
    private final Flux<ServerSentEvent<String>> ping =
            Flux.interval(Duration.ofSeconds(10))
                    .map(tick -> ServerSentEvent.<String>builder()
                            .comment("ping") // comment로 전송하여 클라이언트에서 무시 가능
                            .build());

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "실시간 알림 스트림 연결")
    public Flux<ServerSentEvent<String>> streamNotifications() {
        return SecurityUtils.getCurrentUserId()
                .flatMapMany(userId -> {
                    log.info("SSE 스트림 연결 요청 - 사용자 ID: {}", userId);

                    return Flux.merge(
                                    sseNotificationService.createNotificationStream(userId)
                                            .map(notification -> {
                                                try {
                                                    return ServerSentEvent.<String>builder()
                                                            .id(notification.getIdValue().toString())
                                                            .event("notification")
                                                            .data(objectMapper.writeValueAsString(notification))
                                                            .build();
                                                } catch (Exception e) {
                                                    log.error("알림 직렬화 실패: {}", e.getMessage());
                                                    return ServerSentEvent.<String>builder()
                                                            .event("error")
                                                            .data("알림 처리 중 오류가 발생했습니다.")
                                                            .build();
                                                }
                                            }),
                                    ping
                            )
                            .doOnCancel(() -> {
                                log.info("클라이언트 연결 취소 - 사용자 ID: {}", userId);
                                sseNotificationService.removeConnection(userId);
                            })
                            .doOnComplete(() -> {
                                log.info("SSE 스트림 완료 - 사용자 ID: {}", userId);
                                sseNotificationService.removeConnection(userId);
                            })
                            .doOnError(error -> {
                                log.error("SSE 스트림 에러 - 사용자 ID: {}, 에러: {}", userId, error.getMessage());
                                sseNotificationService.removeConnection(userId);
                            });
                })
                .onErrorResume(error -> {
                    log.error("SSE 연결 생성 실패: {}", error.getMessage());
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data("연결 생성에 실패했습니다.")
                            .build());
                });
    }

    @GetMapping("/connection-status")
    @Operation(summary = "현재 SSE 연결 상태 조회")
    public Mono<ConnectionStatusResponse> getConnectionStatus() {
        return SecurityUtils.getCurrentUserId()
                .map(userId -> new ConnectionStatusResponse(
                        sseNotificationService.isUserConnected(userId),
                        sseNotificationService.getActiveConnectionCount()
                ))
                .defaultIfEmpty(new ConnectionStatusResponse(false, 0));
    }

    public record ConnectionStatusResponse(
            boolean connected,
            int totalConnections
    ) {}
}