package backend.application.port.in.task;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskUseCase {
    record UpdateTaskCommand(
            @NotNull Long projectId,
            @NotNull Long parentId,
            @NotNull Long workspaceId,
            @Nullable List<Long> managerIds,
            @Nullable String taskName,
            @Nullable String taskStatus,
            @Nullable LocalDateTime startDate,
            @Nullable LocalDateTime endDate,
            @Nullable String description,
            @Nullable String fileUrl
    ) {}

    record TaskDetailResult(
            Long taskId,
            String taskName,
            String status,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            String description,
            String fileUrl,
            List<TaskManagerResult> managers
    ) {}

    record TaskManagerResult(
            Long userId,
            String nickname
    ) {}

    Mono<Void> updateTask(@Nullable Long taskId, UpdateTaskCommand command);
    Mono<TaskDetailResult> getTaskDetail(Long taskId);
}