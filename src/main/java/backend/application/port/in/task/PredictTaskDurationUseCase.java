package backend.application.port.in.task;

import backend.domain.task.dto.request.PredictTaskRequest;
import backend.domain.task.dto.response.PredictTaskResponse;
import reactor.core.publisher.Mono;

public interface PredictTaskDurationUseCase {
    Mono<PredictTaskResponse> predict(PredictTaskRequest request);
    Mono<Void> syncTaskHistory();
}
