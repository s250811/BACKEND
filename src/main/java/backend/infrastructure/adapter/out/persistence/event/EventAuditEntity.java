package backend.infrastructure.adapter.out.persistence.event;

import backend.domain.event.audit.EventProcessingStatus;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("event_audit")
public class EventAuditEntity implements Persistable<Long> {

    @Id
    private Long id;

    private Long eventId;
    private String eventType;
    private String consumerTopic;
    private EventProcessingStatus status;
    private String errorMessage;
    @LastModifiedDate
    private LocalDateTime updatedAt;
    @CreatedDate
    private LocalDateTime createdAt;

    @Override
    @Transient
    public boolean isNew() {
        return this.createdAt == null;
    }
}