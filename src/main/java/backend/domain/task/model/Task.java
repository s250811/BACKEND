package backend.domain.task.model;

import backend.application.port.in.task.TaskUseCase;
import backend.domain.common.AggregateRoot;
import backend.domain.common.ChangeDetector;
import backend.domain.task.dto.request.UpdateTaskRequest;
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
        return this.id != null ? this.id.value() : null;
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

    // parentId가 없을 땐 0으로, 필수값 취급
    public static Task merge(Task previousTask, UpdateTaskRequest request) {
        boolean isNew = previousTask == null;
        return Task.builder()
                .id(isNew ? null : previousTask.getId())
                .projectId(request.projectId())
                .parentId(request.parentId() != 0 ? request.parentId() : null)
                .taskName(request.taskName())
                .taskStatus(TaskStatus.fromString(request.taskStatus()))
                .description(request.description())
                .managerIds(request.managerIds() != null ? request.managerIds() : List.of())
                .fileUrl(request.fileUrl())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .isDeleted(false)
                .createdAt(isNew ? LocalDateTime.now() : previousTask.getCreatedAt())
                .previousTask(isNew ? null : previousTask)
                .build();
    }

}
