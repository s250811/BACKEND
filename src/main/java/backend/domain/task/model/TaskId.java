package backend.domain.task.model;

import backend.domain.common.ValueObject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

public record TaskId(Long value) implements ValueObject {
    @JsonCreator
    public TaskId(@JsonProperty("value") Long value) {
        this.value = value;
    }
    public static TaskId of(Long value) {
        return new TaskId(value);
    }
}
