package backend.application.service;

import backend.application.port.in.task.TaskUseCase;
import backend.application.port.out.event.audit.EventAuditRepositoryPort;
import backend.application.port.out.task.TaskManagerRepositoryPort;
import backend.application.port.out.task.TaskRepositoryPort;
import backend.application.port.out.user.UserRepositoryPort;
import backend.application.service.validation.TaskValidationService;
import backend.domain.event.Event;
import backend.domain.event.EventId;
import backend.domain.event.audit.EventAudit;
import backend.domain.event.impl.TaskUpdatedEvent;
import backend.domain.task.dto.request.UpdateTaskRequest;
import backend.domain.task.dto.response.TaskDetailResponse;
import backend.domain.task.model.Task;
import backend.domain.task.model.TaskManager;
import backend.exception.task.TaskErrorCode;
import backend.exception.task.TaskException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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
    private final UserRepositoryPort userRepository;
    private final EventAuditRepositoryPort eventAuditRepository;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Override
    @Transactional
    public Mono<Void> updateTask(@Nullable Long taskId, UpdateTaskRequest request) {
        return validationService.validate(request)
                .then(executeTaskOperation(taskId, request))
                .flatMap(this::saveTaskEventToOutbox)
                .then();
    }

    private Mono<Task> executeTaskOperation(@Nullable Long taskId, UpdateTaskRequest request) {
        if (taskId == null) {
            return Mono.just(Task.merge(null, request))
                    .flatMap(newTask -> updateTaskWithManagers(newTask, request.managerIds()));
        } else {
            return taskRepository.findById(taskId)
                    .switchIfEmpty(Mono.error(new TaskException(TaskErrorCode.TASK_NOT_FOUND)))
                    .map(previousTask -> Task.merge(previousTask, request))
                    .flatMap(mergedTask -> updateTaskWithManagers(mergedTask, request.managerIds()));
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

    // Outbox에 이벤트 저장 (트랜잭션 내부)
    private Mono<Void> saveTaskEventToOutbox(Task savedTask) {
        try {
            Event event = TaskUpdatedEvent.builder().param(savedTask).build();
            String payload = objectMapper.writeValueAsString(event);
            EventAudit audit = EventAudit.createPending(
                    EventId.of(event.getId().value()),
                    event.getType(),
                    payload
            );
            return eventAuditRepository.save(audit).then();
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException(e));
        }
    }


    @Override
    public Mono<TaskDetailResponse> getTaskDetail(Long taskId) {
        return taskRepository.findById(taskId)
                .switchIfEmpty(Mono.error(new TaskException(TaskErrorCode.TASK_NOT_FOUND)))
                .flatMap(task ->
                        taskManagerRepository.findByTaskId(taskId)
                                .map(TaskManager::getUserId)
                                .collectList()
                                .flatMap(managerIds -> {
                                    if (managerIds.isEmpty()) {
                                        return Mono.just(new TaskDetailResponse(
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
                                            .map(user -> new TaskDetailResponse.ManagerResponse(
                                                    user.getIdValue(),
                                                    user.getNickname(),
                                                    user.getProfileImageUrl()
                                            ))
                                            .collectList()
                                            .map(managers -> new TaskDetailResponse(
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
