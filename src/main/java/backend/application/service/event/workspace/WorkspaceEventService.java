package backend.application.service.event.workspace;

import backend.domain.workspace.model.Workspace;
import reactor.core.publisher.Mono;

public interface WorkspaceEventService {
    Mono<Void> publishWorkspaceCreatedEvent(Workspace workspace);
    Mono<Void> publishWorkspaceUpdatedEvent(Workspace workspace);
}
