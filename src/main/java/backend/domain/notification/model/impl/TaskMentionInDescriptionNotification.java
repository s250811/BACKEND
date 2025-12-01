package backend.domain.notification.model.impl;

import backend.domain.event.EventId;
import backend.domain.notification.model.Notification;
import backend.domain.notification.model.NotificationId;
import backend.domain.notification.model.NotificationType;
import backend.domain.task.model.Task;
import backend.domain.user.model.User;
import backend.domain.user.model.UserId;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 작업 본문 멘션 알림
 */
@Getter
public class TaskMentionInDescriptionNotification extends Notification {

    @Builder
    private TaskMentionInDescriptionNotification(NotificationId id, UserId senderId, UserId recipientId, Boolean isRead, EventId eventId, NotificationType type, LocalDateTime createdAt, LocalDateTime readAt, String message, Task param) {
        super(id, senderId, recipientId, isRead, eventId, type, createdAt, readAt, message, param);
    }

    @Override
    public NotificationType getType() {
        return NotificationType.MENTION_IN_TASK_DESCRIPTION;
    }

    @Override
    public String getMessage() {
        return String.format("'%s'본문에서 @당신이 멘션되었어요.", getParam().getTaskName());
    }
}
