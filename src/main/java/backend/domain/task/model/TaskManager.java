package backend.domain.task.model;


import backend.domain.common.AggregateRoot;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;


@Getter
public class TaskManager extends AggregateRoot<TaskManagerId> {

    private Long taskId;

    private Long userId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private TaskManager(TaskManagerId id, Long taskId, Long userId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.taskId = taskId;
        this.userId = userId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getIdValue() {
        return this.id != null ? this.id.value() : null;
    }

}
