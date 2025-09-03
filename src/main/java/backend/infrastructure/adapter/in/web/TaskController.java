package backend.infrastructure.adapter.in.web;

import backend.infrastructure.adapter.in.common.ApiResponseDto;
import backend.application.port.in.TaskUseCase;
import io.swagger.v3.oas.annotations.Operation;
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
    public Mono<ApiResponseDto<Void>> createTask(@RequestBody CreatRequest request) {

        return taskUseCase.createTask(TaskUseCase.from(request))
                .then(Mono.just(ApiResponseDto.createSuccessNoContent(null)));
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

    // 태스크 수정

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

    public record CreatRequest (
            Long projectId,
            Long parentId,
            Long workspaceId,
            List<Long> managerIds,
            String taskName,
            String taskStatus,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String title,
            String description,
            String fileUrl
    ) {}



}
