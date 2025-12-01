package backend.domain.notification.model.impl;

import backend.domain.notification.model.Notification;
import backend.domain.notification.model.NotificationId;
import backend.domain.notification.model.NotificationType;
import backend.domain.task.model.Task;
import backend.domain.user.model.UserId;
import lombok.Builder;

/**
 * 작업 배정 알림
 */
public class TaskAssignedNotification extends Notification {
    @Builder
    private TaskAssignedNotification(NotificationId id, UserId senderId, UserId recipientId, Boolean isRead,
                                    backend.domain.event.EventId eventId,
                                    java.time.LocalDateTime createdAt, java.time.LocalDateTime readAt,
                                    String message, Task param) {
        super(id, senderId, recipientId, isRead, eventId, NotificationType.TASK_ASSIGNED, createdAt, readAt, message, param);
    }

    @Override
    public String getMessage() {
        return String.format("'%s' 태스크가 할당되었어요.", getParam().getTaskName());
    }
}
