package backend.domain.event.impl;

import backend.domain.event.Event;
import backend.domain.event.EventType;
import backend.domain.task.model.Task;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class TaskUpdatedEvent extends Event<Task> {
    @Builder
    public TaskUpdatedEvent(Task param) {
        super(EventType.TASK_UPDATED, param);
    }
}
