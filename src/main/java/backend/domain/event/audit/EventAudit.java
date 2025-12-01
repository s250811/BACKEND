package backend.domain.event.audit;

import backend.domain.common.AggregateRoot;
import backend.domain.event.Event;
import backend.domain.event.EventId;
import backend.domain.event.EventType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class EventAudit extends AggregateRoot<EventAuditId> {

    private EventId eventId;
    private EventType eventType;
    private EventProcessingStatus status;
    private String payload;
    private Integer retryCount;
    private String errorMessage;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;

    @Builder
    private EventAudit(EventAuditId id, EventId eventId, EventType eventType,
                      EventProcessingStatus status, String payload, Integer retryCount,
                      String errorMessage, LocalDateTime updatedAt,
                      LocalDateTime createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.eventType = eventType;
        this.status = status;
        this.payload = payload;
        this.retryCount = retryCount;
        this.errorMessage = errorMessage;
        this.updatedAt = updatedAt;
        this.createdAt = createdAt;
    }

    public Long getIdValue() {
        return this.id != null ? this.id.value() : null;
    }

    public static EventAudit createPending(EventId eventId, EventType eventType, String serializedEvent) {
        return EventAudit.builder()
                .eventId(eventId)
                .eventType(eventType)
                .status(EventProcessingStatus.PENDING)
                .payload(serializedEvent)
                .retryCount(0)
                .build();
    }

    public void markProcessing() {
        this.status = EventProcessingStatus.PROCESSING;
        this.updatedAt = LocalDateTime.now();
    }

    public void markSuccess() {
        this.status = EventProcessingStatus.PUBLISHED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = EventProcessingStatus.FAILED_PUBLISH;
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }

}