package backend.infrastructure.adapter.out.persistence.db.r2dbc.notification;

import backend.domain.notification.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("notification")
public class NotificationEntity implements Persistable<Long> {

    @Id
    private Long id;
    private Long recipientId;
    private Long senderId;
    private Boolean isRead;
    private Long eventId;
    private NotificationType type;
    @CreatedDate
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private String message;

    @Override
    @Transient
    public boolean isNew() {
        return this.createdAt == null;
    }
}