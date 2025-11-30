package backend.domain.workspace.model;

import backend.domain.common.ValueObject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

public record WorkspaceId(Long value) implements ValueObject {
    @JsonCreator
    public WorkspaceId(@JsonProperty("value") Long value) {
        this.value = value;
    }
    public static WorkspaceId of(Long value) {
        return new WorkspaceId(value);
    }
}
