package backend.domain.task.model;

import backend.application.port.in.task.TaskUseCase;
import backend.domain.common.AggregateRoot;
import backend.domain.common.ChangeDetector;
import backend.domain.user.model.UserId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task extends AggregateRoot<TaskId> implements Serializable {
    private TaskId id;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserId lastModifiedBy;
    private Task previousTask;

    public Long getIdValue() {
        return this.id != null ? this.id.getValue() : null;
    }

    public Long getLastModifiedBy() { return this.lastModifiedBy != null ? this.lastModifiedBy.getValue() : null;}

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

    // parentId가 없을 땐 0으로, 필수값 취급
    public static Task merge(Task previousTask, TaskUseCase.UpdateTaskCommand command) {
        boolean isNew = previousTask == null;
        return Task.builder()
                .id(isNew ? null : previousTask.getId())
                .projectId(command.projectId())
                .parentId(command.parentId() != 0 ? command.parentId() : null)
                .taskName(command.taskName())
                .taskStatus(TaskStatus.fromString(command.taskStatus()))
                .description(command.description())
                .managerIds(command.managerIds() != null ? command.managerIds() : List.of())
                .fileUrl(command.fileUrl())
                .startDate(command.startDate())
                .endDate(command.endDate())
                .isDeleted(false)
                .createdAt(isNew ? LocalDateTime.now() : previousTask.getCreatedAt())
                .previousTask(isNew ? null : previousTask)
                .build();
    }

}
