package backend.infrastructure.adapter.in.web.rest.notification;

import backend.application.port.in.notification.NotificationUseCase;
import backend.domain.notification.model.Notification;
import backend.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationUseCase notificationUseCase;

    @GetMapping
    @Operation(summary = "알림 목록 조회")
    public Mono<ResponseEntity<Flux<Notification>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return SecurityUtils.getCurrentUserId()
                .flatMap(userId -> notificationUseCase.getNotificationsByRecipientId(userId, page, size))
                .map(ResponseEntity::ok);
    }

    @PostMapping("/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리")
    public Mono<ResponseEntity<Void>> markAsRead(@PathVariable Long notificationId) {
        return notificationUseCase.markAsRead(notificationId)
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "읽지 않은 알림 개수 조회")
    public Mono<ResponseEntity<Long>> getUnreadCount() {
        return SecurityUtils.getCurrentUserId()
                .flatMap(notificationUseCase::getUnreadCount)
                .map(ResponseEntity::ok);
    }
}