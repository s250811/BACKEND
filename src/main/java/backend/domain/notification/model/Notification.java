package backend.domain.notification.model;

import backend.domain.event.EventId;

import java.time.LocalDateTime;

import backend.domain.common.AggregateRoot;
import backend.domain.task.model.Task;
import backend.domain.user.model.UserId;
import lombok.Getter;

import java.io.Serializable;

@Getter
public class Notification extends AggregateRoot<NotificationId> implements Serializable {
    private UserId senderId;
    private UserId recipientId;
    private Boolean isRead;
    private EventId eventId;
    private NotificationType type;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private String message;
    private Task param;

    public Notification(NotificationId id, UserId senderId, UserId recipientId, Boolean isRead, EventId eventId,
                        NotificationType type, LocalDateTime createdAt, LocalDateTime readAt, String message, Task param) {
        this.id = id;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.isRead = isRead;
        this.eventId = eventId;
        this.type = type;
        this.createdAt = createdAt;
        this.readAt = readAt;
        this.message = message;
        this.param = param;
    }
    public Notification(NotificationId id, UserId senderId, UserId recipientId, Boolean isRead, EventId eventId,
                        NotificationType type, LocalDateTime createdAt, LocalDateTime readAt, String message) {
        this.id = id;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.isRead = isRead;
        this.eventId = eventId;
        this.type = type;
        this.createdAt = createdAt;
        this.readAt = readAt;
        this.message = message;
    }
    public Long getIdValue() {
        return this.getId() != null ? this.getId().value() : null;
    }

    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

}
