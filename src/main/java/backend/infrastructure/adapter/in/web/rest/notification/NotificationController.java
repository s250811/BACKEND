package backend.infrastructure.adapter.in.web.rest.notification;

import backend.application.port.in.notification.NotificationUseCase;
import backend.domain.notification.dto.response.NotificationDetailResponse;
import backend.domain.notification.model.Notification;
import backend.infrastructure.adapter.in.web.rest.dto.ApiResponseDto;
import backend.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.DecimalMax;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationUseCase notificationUseCase;

    @GetMapping
    @Operation(summary = "알림 목록 조회")
    public Mono<ApiResponseDto<List<NotificationDetailResponse>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return SecurityUtils.getCurrentUserId()
                .flatMap(userId -> notificationUseCase.getNotificationsByRecipientId(userId, page, size)
                        .collectList())
                .map(notifications -> ApiResponseDto.createSuccess(notifications, "알림 목록 조회 완료"));
    }

    @PostMapping("/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리")
    public Mono<ApiResponseDto<Void>> markAsRead(@PathVariable Long notificationId) {
        return notificationUseCase.markAsRead(notificationId)
                .thenReturn(ApiResponseDto.createSuccessNoContent("알림 읽음 처리 완료"));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "읽지 않은 알림 개수 조회")
    public Mono<ApiResponseDto<Long>> getUnreadCount() {
        return SecurityUtils.getCurrentUserId()
                .flatMap(notificationUseCase::getUnreadCount)
                .map(count -> ApiResponseDto.createSuccess(count, "읽지 않은 알림 개수 조회 완료"));
    }
}