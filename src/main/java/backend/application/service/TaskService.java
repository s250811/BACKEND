package backend.application.service;

import backend.application.port.in.task.TaskUseCase;
import backend.application.port.out.messaging.EventProducerPort;
import backend.application.port.out.task.TaskManagerRepositoryPort;
import backend.application.port.out.task.TaskRepositoryPort;
import backend.application.port.out.user.UserRepositoryPort;
import backend.application.service.validation.TaskValidationService;
import backend.domain.event.Event;
import backend.domain.event.impl.TaskUpdatedEvent;
import backend.domain.task.model.Task;
import backend.domain.task.model.TaskManager;
import backend.exception.task.TaskErrorCode;
import backend.exception.task.TaskException;
import backend.exception.workspace.WorkspaceErrorCode;
import backend.exception.workspace.WorkspaceException;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService implements TaskUseCase {

    private final TaskValidationService validationService;
    private final TaskRepositoryPort taskRepository;
    private final TaskManagerRepositoryPort taskManagerRepository;
    private final EventProducerPort eventPublisher;
    private final UserRepositoryPort userRepository;

    @Override
    @Transactional
    public Mono<Void> updateTask(@Nullable Long taskId, UpdateTaskCommand command) {
        return validationService.validate(command)
                .then(executeTaskOperation(taskId, command))
                .flatMap(this::publishTaskUpdatedEvent);
    }

    private Mono<Task> executeTaskOperation(@Nullable Long taskId, UpdateTaskCommand command) {
        if (taskId == null) {
            return Mono.just(Task.merge(null, command))
                    .flatMap(newTask -> updateTaskWithManagers(newTask, command.managerIds()));
        } else {
            return taskRepository.findById(taskId)
                    .switchIfEmpty(Mono.error(new TaskException(TaskErrorCode.TASK_NOT_FOUND)))
                    .map(previousTask -> Task.merge(previousTask, command))
                    .flatMap(mergedTask -> updateTaskWithManagers(mergedTask, command.managerIds()));
        }
    }

    protected Mono<Task> updateTaskWithManagers(Task task, List<Long> managerIds) {
        return taskRepository.save(task)
                .flatMap(savedTask -> taskManagerRepository.deleteByTaskId(savedTask.getIdValue())
                        .then(Flux.fromIterable(managerIds.stream().distinct().toList())
                                .flatMap(managerId ->
                                        taskManagerRepository.save(TaskManager.builder()
                                                .taskId(savedTask.getIdValue())
                                                .userId(managerId)
                                                .build())
                                )
                                .then())
                        .thenReturn(savedTask));
    }

    // TODO: 엔티티 변경 시점부터 아웃박스 테이블인 event_audit에 기록하도록 변경 필요
    private Mono<Void> publishTaskUpdatedEvent(Task savedTask) {
        Event event = TaskUpdatedEvent.builder()
                .param(savedTask)
                .build();
        return eventPublisher.publishEvent(event);
    }

    @Override
    public Mono<TaskDetailResult> getTaskDetail(Long taskId) {
        return taskRepository.findById(taskId)
                .switchIfEmpty(Mono.error(new TaskException(TaskErrorCode.TASK_NOT_FOUND)))
                .flatMap(task ->
                        taskManagerRepository.findByTaskId(taskId)
                                .map(TaskManager::getUserId)
                                .collectList()
                                .flatMap(managerIds -> {
                                    if (managerIds.isEmpty()) {
                                        return Mono.just(new TaskDetailResult(
                                                task.getIdValue(),
                                                task.getTaskName(),
                                                task.getTaskStatus().name(),
                                                task.getStartDate(),
                                                task.getEndDate(),
                                                task.getDescription(),
                                                task.getFileUrl(),
                                                List.of()
                                        ));
                                    }
                                    return userRepository.findAllById(managerIds)
                                            .map(user -> new TaskManagerResult(
                                                    user.getIdValue(),
                                                    user.getNickname(),
                                                    user.getProfileImageUrl()
                                            ))
                                            .collectList()
                                            .map(managers -> new TaskDetailResult(
                                                    task.getIdValue(),
                                                    task.getTaskName(),
                                                    task.getTaskStatus().name(),
                                                    task.getStartDate(),
                                                    task.getEndDate(),
                                                    task.getDescription(),
                                                    task.getFileUrl(),
                                                    managers
                                            ));
                                })
                );
    }
}
