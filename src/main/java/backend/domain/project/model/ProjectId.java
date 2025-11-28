package backend.domain.project.model;

import backend.domain.common.ValueObject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

public record ProjectId(Long value) implements ValueObject {
    @JsonCreator
    public ProjectId(@JsonProperty("value") Long value) {
        this.value = value;
    }
    public static ProjectId of(Long id) {
        return new ProjectId(id);
    }
}
