package backend.domain.notification.model.impl;

import backend.domain.notification.model.Notification;
import backend.domain.notification.model.NotificationId;
import backend.domain.notification.model.NotificationType;
import backend.domain.task.model.Task;
import backend.domain.user.model.UserId;
import lombok.Builder;

/**
 * 작업 속성 변경 알림
 */
public class TaskFieldsChangedNotification extends Notification {
    @Builder
    public TaskFieldsChangedNotification(NotificationId id, UserId senderId, UserId recipientId, Boolean isRead,
                                         backend.domain.event.EventId eventId,
                                         java.time.LocalDateTime createdAt, java.time.LocalDateTime readAt,
                                         String message, Task param) {
        super(id, senderId, recipientId, isRead, eventId, NotificationType.TASK_FIELDS_CHANGED, createdAt, readAt, message, param);
    }

    @Override
    public String getMessage() {
        return String.format("'%s'의 '%s'이 변경되었어요.", getParam().getTaskName(), getParam().collectChangedFields(getParam().getPreviousTask()));
    }
}

