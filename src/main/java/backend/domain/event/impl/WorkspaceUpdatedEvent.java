package backend.domain.event.impl;

import backend.domain.event.Event;
import backend.domain.event.EventType;
import backend.domain.workspace.model.Workspace;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor

public class WorkspaceUpdatedEvent extends Event<Workspace> {
    @Builder
    public WorkspaceUpdatedEvent(Workspace param) {
        super(EventType.WORKSPACE_UPDATED, param);
    }
}
