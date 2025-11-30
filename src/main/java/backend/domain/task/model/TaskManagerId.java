package backend.domain.task.model;

import backend.domain.common.ValueObject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

public record TaskManagerId(Long value) implements ValueObject {
    @JsonCreator
    public TaskManagerId(@JsonProperty("value") Long value) {
        this.value = value;
    }
    public static TaskManagerId of(Long id) {
        return new TaskManagerId(id);
    }
}
