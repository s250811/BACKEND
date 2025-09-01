package backend.domain.task.model;

import backend.domain.common.ValueObject;
import lombok.Getter;

@Getter
public class TaskId extends ValueObject {

    private final Long value;

    public TaskId(Long value) {
        this.value = value;
    }

    public static TaskId of(Long id) {
        return new TaskId(id);
    }
}
