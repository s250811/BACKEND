package backend.application.service.event.workspace;

import backend.domain.event.Event;
import backend.domain.event.EventTopic;
import backend.domain.event.EventType;
import backend.domain.workspace.model.Workspace;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor

public class WorkspaceUpdatedEvent extends Event<Workspace> {


    @Builder
    public WorkspaceUpdatedEvent(Workspace param) {
        super(EventType.WORKSPACE_UPDATED, param, EventTopic.WORKSPACE_TOPIC);
    }

    public static WorkspaceUpdatedEvent create(Workspace workspace) {
        return WorkspaceUpdatedEvent.builder()
                .param(workspace)
                .build();
    }

    @Override
    public String getPartitionKey() {
        return getParam().getId().getValue().toString();
    }
}
