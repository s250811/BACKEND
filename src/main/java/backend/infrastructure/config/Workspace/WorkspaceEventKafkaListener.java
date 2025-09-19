package backend.infrastructure.config.Workspace;

import backend.domain.event.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class WorkspaceEventKafkaListener {

    private final WorkspaceEventProcessor eventProcessor;

    @KafkaListener(topics = {"task-events", "project-events", "workspace-events"}, groupId = "pm-backend")
    public Mono<Void> handleWorkspaceEvent(
            @Payload Event<?> event,
            @Header("workspaceId") Long workspaceId) {

        log.info("Received Kafka event: {} for workspace: {}", event.getId(), workspaceId);

        return eventProcessor.processEvent(event, workspaceId)
                .doOnSuccess(v -> log.debug("Successfully processed event: {}", event.getId()))
                .doOnError(err -> log.error("Failed to process event: {}", event.getId(), err));
    }
}
