package backend.domain.task.model;

import backend.domain.common.ValueObject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TaskId extends ValueObject {

    private Long value;
    @JsonCreator
    public TaskId(Long value) {
        this.value = value;
    }

    public static TaskId of(Long id) {
        return new TaskId(id);
    }
}
