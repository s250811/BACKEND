package backend.domain.workspaceInvite.model;

import backend.domain.common.ValueObject;
import lombok.Getter;

@Getter
public class WorkSpaceInviteId extends ValueObject {

    private final Long value;

    public WorkSpaceInviteId(Long value) {
        this.value = value;
    }

    public static WorkSpaceInviteId of(Long id) {
        return new WorkSpaceInviteId(id);
    }
}
