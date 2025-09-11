package backend.domain.notification.model.impl;

import backend.domain.event.EventId;
import backend.domain.notification.model.Notification;
import backend.domain.notification.model.NotificationId;
import backend.domain.notification.model.NotificationType;
import backend.domain.task.model.Task;
import backend.domain.user.model.UserId;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 댓글 멘션 알림
 */
public class CommentMentionNotification extends Notification {
    @Builder
    public CommentMentionNotification(NotificationId id, UserId recipientId, UserId senderId, Boolean isRead,
                                      EventId eventId, LocalDateTime createdAt, LocalDateTime readAt,
                                      String message, Task param) {
        super(id, recipientId, senderId, isRead, eventId, NotificationType.MENTION_IN_COMMENT, createdAt, readAt, message, param);
    }

    @Override
    public String getMessage() {
        return String.format("'%s' 댓글에서 @당신이 멘션되었어요.", getParam().getTaskName());
    }
}
