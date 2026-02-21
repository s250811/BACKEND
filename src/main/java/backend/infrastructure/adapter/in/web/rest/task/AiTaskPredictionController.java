package backend.infrastructure.adapter.in.web.rest.task;

import backend.application.port.in.task.PredictTaskDurationUseCase;
import backend.domain.task.dto.request.PredictTaskRequest;
import backend.domain.task.dto.response.PredictTaskResponse;
import backend.infrastructure.adapter.in.web.rest.dto.ApiResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Slf4j
public class AiTaskPredictionController {
    private final PredictTaskDurationUseCase predictTaskDurationUseCase;

    @Operation(summary = "업무 소요 기간 예측")
    @PostMapping("/predict")
    public Mono<ApiResponseDto<PredictTaskResponse>> predictTaskDuration(@Valid @RequestBody PredictTaskRequest request) {
        return predictTaskDurationUseCase.predict(request)
                .map(response -> ApiResponseDto.createSuccess(response, "소요 기간 예측 완료"));
    }

    @Operation(summary = "AI 서버에 업무 이력 동기화")
    @PostMapping("/sync-history")
    public Mono<ApiResponseDto<Void>> syncTaskHistory() {
        return predictTaskDurationUseCase.syncTaskHistory()
                .thenReturn(ApiResponseDto.createSuccessNoContent("업무 이력 동기화 완료"));
    }

}
