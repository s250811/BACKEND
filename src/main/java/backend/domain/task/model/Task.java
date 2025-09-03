package backend.domain.task.model;

import backend.domain.common.AggregateRoot;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
public class Task extends AggregateRoot<TaskId> {

    private Long projectId;

    private Long parentId;

    private String taskName;

    private TaskStatus taskStatus;

    private boolean isDeleted;

    private String description;

    private List<Long> managerIds;

    private String fileUrl;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public Task(TaskId id, Long projectId, Long parentId, String taskName, TaskStatus taskStatus,
                boolean isDeleted, String description,List<Long> managerIds,
                String fileUrl, LocalDateTime startDate, LocalDateTime endDate,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.projectId = projectId;
        this.parentId = parentId;
        this.taskName = taskName;
        this.taskStatus = taskStatus;
        this.isDeleted = isDeleted;
        this.description = description;
        this.managerIds = initializeManagerIds(managerIds);
        this.fileUrl = fileUrl;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    private List<Long> initializeManagerIds(List<Long> managerIds) {
        return (managerIds != null) ? managerIds : new ArrayList<>();
    }


    public Long getIdValue() {
        return this.id != null ? this.id.getValue() : null;
    }
}
