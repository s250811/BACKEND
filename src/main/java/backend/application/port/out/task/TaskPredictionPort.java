package backend.application.port.out.task;

import backend.domain.task.model.TaskPrediction;
import reactor.core.publisher.Mono;

public interface TaskPredictionPort {
    Mono<TaskPrediction> predictDuration(String taskName, String description, Long assigneeId, Integer topK);
}
