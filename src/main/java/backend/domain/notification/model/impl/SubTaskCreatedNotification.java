package backend.domain.notification.model.impl;

import backend.domain.notification.model.Notification;
import backend.domain.notification.model.NotificationId;
import backend.domain.notification.model.NotificationType;
import backend.domain.task.model.Task;
import backend.domain.user.model.UserId;
import lombok.Builder;
import org.apache.kafka.common.protocol.types.Field;

/**
 * 하위 작업 생성 알림
 */
public class SubTaskCreatedNotification extends Notification {
    @Builder
    public SubTaskCreatedNotification(NotificationId id, UserId senderId, UserId recipientId, Boolean isRead,
                                      backend.domain.event.EventId eventId,
                                      java.time.LocalDateTime createdAt, java.time.LocalDateTime readAt,
                                      String message, Task param) {
        super(id, senderId, recipientId, isRead, eventId, NotificationType.SUBTASK_CREATED, createdAt, readAt, message, param);
    }

    @Override
    public String getMessage() {
        return String.format("'%s'에 새로운 하위 태스크가 생성되었어요.", getParam().getTaskName());
    }
}

