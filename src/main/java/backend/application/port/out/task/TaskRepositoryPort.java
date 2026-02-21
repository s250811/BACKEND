package backend.application.port.out.task;


import backend.domain.project.model.ProjectId;
import backend.domain.task.model.Task;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface TaskRepositoryPort {
    Mono<Task> findById(Long id);
    Mono<Task> save(Task task);
    Flux<Task> findAllByProjectIdIn(List<ProjectId> projectIds);
    Flux<Task> findAllCompletedTasks();
}
