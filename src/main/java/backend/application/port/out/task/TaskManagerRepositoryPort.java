package backend.application.port.out.task;

import backend.domain.task.model.TaskManager;
import reactor.core.publisher.Mono;

public interface TaskManagerRepositoryPort {
    Mono<TaskManager> save(TaskManager taskManager);
}
