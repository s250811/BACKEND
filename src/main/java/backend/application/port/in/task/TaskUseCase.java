package backend.application.port.in.task;

import backend.domain.task.dto.request.UpdateTaskRequest;
import backend.domain.task.dto.response.TaskDetailResponse;
import jakarta.annotation.Nullable;
import reactor.core.publisher.Mono;

public interface TaskUseCase {
    Mono<Void> updateTask(@Nullable Long taskId, UpdateTaskRequest command);
    Mono<TaskDetailResponse> getTaskDetail(Long taskId);
}