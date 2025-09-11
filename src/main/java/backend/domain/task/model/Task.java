package backend.domain.task.model;

import backend.application.port.in.TaskUseCase;
import backend.domain.common.AggregateRoot;
import backend.domain.common.ChangeDetector;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

@Getter
@NoArgsConstructor
public class Task extends AggregateRoot<TaskId> implements Serializable {

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
    private Task previousTask;

    @Builder
    public Task(TaskId id, Long projectId, Long parentId, String taskName, TaskStatus taskStatus,
                boolean isDeleted, String description,List<Long> managerIds,
                String fileUrl, LocalDateTime startDate, LocalDateTime endDate,
                LocalDateTime createdAt, LocalDateTime updatedAt, Task previousTask) {
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
        this.previousTask = previousTask;
    }

    private List<Long> initializeManagerIds(List<Long> managerIds) {
        return (managerIds != null) ? managerIds : new ArrayList<>();
    }

    public Long getIdValue() {
        return this.id != null ? this.id.getValue() : null;
    }

    public List<Long> extractMentionedUserIds() {
        if (description == null || description.isEmpty()) {
            return List.of();
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@mention\\[(\\d+)\\]");
        java.util.regex.Matcher matcher = pattern.matcher(description);

        return matcher.results()
                .map(matchResult -> Long.parseLong(matchResult.group(1)))
                .distinct()
                .toList();
    }
    public boolean isStatusChanged(Task previous) {
        return ChangeDetector.isFieldChanged(previous, this, "taskStatus");
    }

    public String collectChangedFields(Task previous) {
        return ChangeDetector.getChangedFieldsAsString(previous, this, "previousTask", "managerIds", "taskStatus");
    }

    public boolean hasDescriptionMentions() {
        return !extractMentionedUserIds().isEmpty();
    }
    public Task updateWith(TaskUseCase.UpdateTaskCommand command, Task previous) {
        return Task.builder()
                .id(this.id)
                .projectId(command.projectId())
                .parentId(command.parentId())
                .taskName(command.taskName())
                .taskStatus(TaskStatus.fromString(command.taskStatus()))
                .description(command.description() != null ? command.description() : this.description)
                .managerIds(command.managerIds() != null ? command.managerIds() : this.managerIds)
                .fileUrl(command.fileUrl() != null ? command.fileUrl() : this.fileUrl)
                .startDate(command.startDate() != null ? command.startDate() : this.startDate)
                .endDate(command.endDate() != null ? command.endDate() : this.endDate)
                .createdAt(this.createdAt)
                .previousTask(previous)
                .build();
    }
}
