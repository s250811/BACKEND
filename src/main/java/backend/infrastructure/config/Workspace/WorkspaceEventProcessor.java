package backend.infrastructure.config.Workspace;

import backend.domain.event.Event;
import reactor.core.publisher.Mono;

public interface WorkspaceEventProcessor {
    Mono<Void> processEvent(Event<?> event, Long workspaceId);
}
