package backend.domain.workspaceMember.model;

import backend.domain.common.ValueObject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

public record WorkspaceMemberId(Long value) implements ValueObject {
    @JsonCreator
    public WorkspaceMemberId(@JsonProperty("value") Long value) {
        this.value = value;
    }
    public static WorkspaceMemberId of(Long value) {
        return new WorkspaceMemberId(value);
    }
}
