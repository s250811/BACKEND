package backend.domain.event.audit;

import backend.domain.common.AggregateRoot;
import backend.domain.event.EventId;
import backend.domain.event.EventType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class EventAudit extends AggregateRoot<EventAuditId> {

    private EventId eventId;
    private EventType eventType;
    private String consumerTopic;
    private EventProcessingStatus status;
    private String errorMessage;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;

    @Builder
    public EventAudit(EventAuditId id, EventId eventId, EventType eventType,
                      String consumerTopic, EventProcessingStatus status,
                      String errorMessage, LocalDateTime updatedAt,
                      LocalDateTime createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.eventType = eventType;
        this.consumerTopic = consumerTopic;
        this.status = status;
        this.errorMessage = errorMessage;
        this.updatedAt = updatedAt;
        this.createdAt = createdAt;
    }

    public Long getIdValue() {
        return this.id != null ? this.id.getValue() : null;
    }

    public static EventAudit createStarted(EventId eventId, EventType eventType, String consumerTopic) {
        return EventAudit.builder()
                .eventId(eventId)
                .eventType(eventType)
                .consumerTopic(consumerTopic)
                .status(EventProcessingStatus.PROCESSING)
                .build();
    }

    public void markSuccess() {
        this.status = EventProcessingStatus.SUCCESS;
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = EventProcessingStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }
}