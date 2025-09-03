package backend.domain.workspace.model;

import backend.domain.common.ValueObject;
import lombok.Getter;

@Getter
public class WorkspaceId extends ValueObject {

    private final Long value;

    public WorkspaceId(Long value) {
        this.value = value;
    }

    public static WorkspaceId of(Long id) {
        return new WorkspaceId(id);
    }
}
