package backend.domain.workspaceInvite.model;

import backend.domain.common.ValueObject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

public record WorkSpaceInviteId(Long value) implements ValueObject {
    @JsonCreator
    public WorkSpaceInviteId(@JsonProperty("value") Long value) {
        this.value = value;
    }
    public static WorkSpaceInviteId of(Long id) {
        return new WorkSpaceInviteId(id);
    }
}
