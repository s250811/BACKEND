package backend.domain.event.audit;

public enum EventProcessingStatus {
    PENDING, PROCESSING, PUBLISHED, FAILED_PUBLISH
}
