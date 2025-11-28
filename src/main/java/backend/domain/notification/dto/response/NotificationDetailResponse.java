package backend.domain.notification.dto.response;

import backend.domain.notification.model.NotificationType;

import java.time.LocalDateTime;

public record NotificationDetailResponse(
        Long id,
        Long senderId,
        Long recipientId,
        Boolean isRead,
        NotificationType type,
        LocalDateTime createdAt,
        LocalDateTime readAt,
        String message
) {}