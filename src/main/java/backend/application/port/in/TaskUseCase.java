package backend.application.port.in;

import backend.infrastructure.adapter.in.web.TaskController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskUseCase {
    Mono<Void> createTask(UpdateTaskCommand command);
    Mono<TaskDetailResult> getTaskDetail(Long taskId);
    Mono<Void> updateTask(Long taskId, UpdateTaskCommand command);

    record UpdateTaskCommand(
            Long projectId,
            Long workspaceId,
            Long parentId,
            List<Long> managerIds,
            String taskName,
            String taskStatus,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String description,
            String fileUrl
    ) {}

    record TaskDetailResult (
            Long taskId,
            String taskName,
            String status,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            String description,
            String fileUrl,
            List<ManagerResponse> managers
    ) {
        public record ManagerResponse(Long userId, String nickname) {}
    }

    public static UpdateTaskCommand from(TaskController.CreatRequest request) {
        return new UpdateTaskCommand(
                request.projectId(),
                request.workspaceId(),
                request.parentId(),
                request.managerIds(),
                request.taskName(),
                request.taskStatus(),
                request.startDate(),
                request.endDate(),
                request.description(),
                request.fileUrl()
        );
    }

    record GetTaskResult (
            Long taskId,
            Long projectId,
            Long parentId,
            Long workspaceId,
            List<String> managerNicknames,
            String taskName,
            String taskStatus,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String description,
            String fileUrl
    ) {}
}
