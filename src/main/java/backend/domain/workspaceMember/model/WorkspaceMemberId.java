package backend.domain.workspaceMember.model;

import backend.domain.common.ValueObject;
import lombok.Getter;

@Getter
public class WorkspaceMemberId extends ValueObject {

    private final Long value;

    public WorkspaceMemberId(Long value) {
        this.value = value;
    }

    public static WorkspaceMemberId of(Long id) {
        return new WorkspaceMemberId(id);
    }
}
