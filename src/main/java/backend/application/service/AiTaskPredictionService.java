package backend.application.service;

import backend.application.port.in.task.PredictTaskDurationUseCase;
import backend.application.port.out.task.TaskHistoryPort;
import backend.application.port.out.task.TaskPredictionPort;
import backend.domain.task.dto.request.PredictTaskRequest;
import backend.domain.task.dto.response.PredictTaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiTaskPredictionService implements PredictTaskDurationUseCase {
    private final TaskHistoryPort taskHistoryPort;
    private final TaskPredictionPort taskPredictionPort;
    @Override
    public Mono<PredictTaskResponse> predict(PredictTaskRequest request) {
        return taskPredictionPort.predictDuration(request.taskName(), request.description(), request.assigneeId(), request.similarTaskCount()
                ).flatMap(prediction ->{
                            return Mono.just(
                                    PredictTaskResponse.builder()
                                            .predictedDurationDays(prediction.predictedDurationDays())
                                            .similarTasks(prediction.similarTasks())
                                            .build()
                            );
                        }

        );
    }

    @Override
    public Mono<Void> syncTaskHistory() {
        return taskHistoryPort.syncTaskHistoryToAi()
                .doOnSuccess(v -> log.info("Task history sync completed"))
                .doOnError(error -> log.error("Task history sync failed", error));
    }
}
