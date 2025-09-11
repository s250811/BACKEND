package backend.domain.notification.model.impl;

import backend.domain.notification.model.Notification;
import backend.domain.notification.model.NotificationId;
import backend.domain.notification.model.NotificationType;
import backend.domain.task.model.Task;
import backend.domain.user.model.UserId;
import lombok.Builder;

/**
 * 작업 상태 변경 알림
 */
public class TaskStatusChangedNotification extends Notification {
    @Builder
    public TaskStatusChangedNotification(NotificationId id, UserId recipientId, UserId senderId, Boolean isRead,
                                         backend.domain.event.EventId eventId,
                                         java.time.LocalDateTime createdAt, java.time.LocalDateTime readAt,
                                         String message, Task param) {
        super(id, recipientId, senderId, isRead, eventId, NotificationType.TASK_STATUS_CHANGED, createdAt, readAt, message, param);
    }

    @Override
    public String getMessage() {
        return String.format("'%s'의 상태가 '%s'(으)로 변경되었어요.", getParam().getTaskName(), getParam().getTaskStatus().getDisplayName());
    }
}