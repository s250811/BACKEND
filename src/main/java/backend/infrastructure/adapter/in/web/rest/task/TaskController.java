package backend.infrastructure.adapter.in.web.rest.task;

import backend.domain.task.dto.request.UpdateTaskRequest;
import backend.domain.task.dto.response.TaskDetailResponse;
import backend.infrastructure.adapter.in.web.rest.dto.ApiResponseDto;
import backend.application.port.in.task.TaskUseCase;
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
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskUseCase taskUseCase;

    @Operation(summary = "태스크 생성")
    @PostMapping
    public Mono<ApiResponseDto<Void>> createTask(@RequestBody UpdateTaskRequest request) {
        return taskUseCase.updateTask(null, request)
                .then(Mono.just(ApiResponseDto.createSuccess(null, "태스크가 생성되었습니다.")));
    }

    @Operation(summary = "태스크 수정")
    @PutMapping("/{taskId}") // Full Update
    public Mono<ApiResponseDto<Void>> updateTask(@PathVariable Long taskId, @RequestBody UpdateTaskRequest request) {
        return taskUseCase.updateTask(taskId, request)
                .then(Mono.just(ApiResponseDto.createSuccess(null, "태스크가 수정되었습니다.")));
    }

    @Operation(summary = "태스크 조회")
    @GetMapping("/{taskId}")
    public Mono<ApiResponseDto<TaskDetailResponse>> getTaskDetail(@PathVariable Long taskId) {
        return taskUseCase.getTaskDetail(taskId)
                .map(response -> ApiResponseDto.createSuccess(response, "태스크 조회 완료"));
    }
}