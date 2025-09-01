package backend.domain.task.model;

import backend.domain.common.ValueObject;
import lombok.Getter;

@Getter
public class TaskManagerId extends ValueObject {

    private final Long value;

    public TaskManagerId(Long value) {
        this.value = value;
    }

    public static TaskManagerId of(Long id) {
        return new TaskManagerId(id);
    }
}
