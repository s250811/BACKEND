package backend.application.service.event.workspace;

import backend.domain.event.Event;
import backend.domain.event.EventTopic;
import backend.domain.event.EventType;
import backend.domain.workspace.model.Workspace;
import lombok.Builder;
import lombok.NoArgsConstructor;


@NoArgsConstructor
public class WorkspaceCreatedEvent extends Event<Workspace> {

    @Builder
    public WorkspaceCreatedEvent(Workspace param) {
        super(EventType.WORKSAPCE_CREATED, param, EventTopic.WORKSPACE_TOPIC);

    }

    public static WorkspaceCreatedEvent create(Workspace workspace) {
        return WorkspaceCreatedEvent.builder()
                .param(workspace)
                .build();
    }


    @Override
    public String getPartitionKey() {
        return getParam().getId().getValue().toString();
    }
}
