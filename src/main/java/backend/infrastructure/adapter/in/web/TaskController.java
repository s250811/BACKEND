package backend.infrastructure.adapter.in.web;

import backend.infrastructure.adapter.in.common.ApiResponseDto;
import backend.application.port.in.TaskUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TaskController {

    private final TaskUseCase taskUseCase;

    @Operation(summary = "태스크 생성")
    @PostMapping("/tasks")
    public Mono<ApiResponseDto<Void>> createTask(@RequestBody TaskRequest request) {
        TaskUseCase.UpdateTaskCommand command =
                new TaskUseCase.UpdateTaskCommand(
                        request.projectId(),
                        request.parentId(),
                        request.workspaceId(),
                        request.managerIds(),
                        request.taskName(),
                        request.taskStatus(),
                        request.startDate(),
                        request.endDate(),
                        request.description(),
                        request.fileUrl()
                );
        return taskUseCase.updateTask(null, command)
                .then(Mono.just(ApiResponseDto.createSuccess(null, "태스크가 생성되었습니다.")));
    }

    @Operation(summary = "태스크 수정")
    @PutMapping("/tasks/{taskId}") // Full Update
    public Mono<ApiResponseDto<Void>> updateTask(
            @PathVariable Long taskId,
            @RequestBody TaskRequest request) {
        TaskUseCase.UpdateTaskCommand command =
                new TaskUseCase.UpdateTaskCommand(
                        request.projectId(),
                        request.parentId(),
                        request.workspaceId(),
                        request.managerIds(),
                        request.taskName(),
                        request.taskStatus(),
                        request.startDate(),
                        request.endDate(),
                        request.description(),
                        request.fileUrl()
                );
        return taskUseCase.updateTask(taskId, command)
                .then(Mono.just(ApiResponseDto.createSuccess(null, "태스크가 수정되었습니다.")));
    }

    @Operation(summary = "태스크 상세 조회")
    @GetMapping("/tasks/{taskId}")
    public Mono<ApiResponseDto<TaskDetailResponse>> getTaskDetail(@PathVariable Long taskId) {
        return taskUseCase.getTaskDetail(taskId)
                .map(result -> new TaskDetailResponse(
                        result.taskId(),
                        result.taskName(),
                        result.status(),
                        result.startedAt(),
                        result.endedAt(),
                        result.description(),
                        result.fileUrl(),
                        result.managers().stream()
                                .map(m -> new TaskDetailResponse.ManagerResponse(m.userId(), m.nickname()))
                                .toList()
                ))
                .map(response -> ApiResponseDto.createSuccess(response, "태스크 조회 완료"));
    }

    public record TaskDetailResponse(
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

    public record TaskRequest(
            @NotNull
            Long projectId,
            @NotNull
            @Schema(description = "부모 태스크 ID (루트 태스크인 경우 0)", example = "0")
            Long parentId,
            @NotNull
            Long workspaceId,
            @Schema(example = "[1, 2, 3]")
            @Nullable
            List<Long> managerIds,
            @Nullable
            String taskName,
            @Nullable
            String taskStatus,
            @Nullable
            LocalDateTime startDate,
            @Nullable
            LocalDateTime endDate,
            @Nullable
            String description,
            @Nullable
            String fileUrl
    ) {}
}