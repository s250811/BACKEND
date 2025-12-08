package backend.infrastructure.adapter.out.persistence.db.r2dbc.task;

import backend.application.port.out.task.TaskRepositoryPort;
import backend.domain.project.model.ProjectId;
import backend.domain.task.model.Task;
import backend.domain.task.model.TaskId;
import backend.domain.task.model.TaskStatus;
import backend.domain.user.model.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TaskPersistenceAdapter implements TaskRepositoryPort {

    private final TaskR2dbcRepository repository;

    @Override
    public Mono<Task> findById(Long id) {
        return repository.findById(id)
                .map(TaskPersistenceAdapter::toDomain);
    }

    @Override
    public Mono<Task> save(Task task) {
        TaskEntity entity = toEntity(task);
        return repository.save(entity)
                .map(TaskPersistenceAdapter::toDomain);
    }

    @Override
    public Flux<Task> findAllByProjectIdIn(List<ProjectId> projectIds) {
        List<Long> ids = projectIds.stream().map(ProjectId::value).toList();
        return repository.findAllByProjectIdIn(ids)
                .map(TaskPersistenceAdapter::toDomain);
    }

    @Override
    public Flux<Task> findAllCompletedTasks() {
        return repository.findAllByTaskStatusAndIsDeletedAndStartDateIsNotNullAndEndDateIsNotNullOrderByUpdatedAtDesc(
                        TaskStatus.DONE,
                        false
                )
                .map(TaskPersistenceAdapter::toDomain);
    }

    private static TaskEntity toEntity(Task task) {
        return TaskEntity.builder()
                .id(task.getId() != null ? task.getIdValue() : null)
                .projectId(task.getProjectId())
                .parentId(task.getParentId())
                .taskName(task.getTaskName())
                .taskStatus(task.getTaskStatus())
                .description(task.getDescription())
                .managerIds(task.getManagerIds())
                .fileUrl(task.getFileUrl())
                .startDate(task.getStartDate())
                .endDate(task.getEndDate())
                .isDeleted(task.isDeleted())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private static Task toDomain(TaskEntity entity) {
        return Task.builder()
                .id(TaskId.of(entity.getId()))
                .projectId(entity.getProjectId())
                .parentId(entity.getParentId())
                .taskName(entity.getTaskName())
                .taskStatus(entity.getTaskStatus())
                .description(entity.getDescription())
                .managerIds(entity.getManagerIds())
                .fileUrl(entity.getFileUrl())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .isDeleted(entity.isDeleted())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .lastModifiedBy(UserId.of(entity.getLastModifiedBy()))
                .build();
    }
}
