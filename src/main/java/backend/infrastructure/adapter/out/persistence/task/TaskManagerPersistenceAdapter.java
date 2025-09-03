package backend.infrastructure.adapter.out.persistence.task;

import backend.application.port.out.task.TaskManagerRepositoryPort;
import backend.domain.task.model.TaskManager;
import backend.domain.task.model.TaskManagerId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TaskManagerPersistenceAdapter implements TaskManagerRepositoryPort {

    private final TaskManagerR2dbcRepository repository;
    @Override
    public Mono<TaskManager> save(TaskManager taskManager) {
        TaskManagerEntity entity = toEntity(taskManager);
        return repository.save(entity)
                .map(TaskManagerPersistenceAdapter::toDomain);
    }

    public static TaskManager toDomain(TaskManagerEntity entity) {
        return TaskManager.builder()
                .id(TaskManagerId.of(entity.getId()))
                .taskId(entity.getTaskId())
                .userId(entity.getUserId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static TaskManagerEntity toEntity(TaskManager taskManager) {
        return TaskManagerEntity.builder()
                .id(taskManager.getIdValue())
                .taskId(taskManager.getTaskId())
                .userId(taskManager.getUserId())
                .createdAt(taskManager.getCreatedAt())
                .updatedAt(taskManager.getUpdatedAt())
                .build();
    }
}
