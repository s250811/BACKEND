package backend.application.service;

import backend.application.port.in.TaskUseCase;
import backend.application.port.out.event.EventPublishingPort;
import backend.application.port.out.folder.FolderRepositoryPort;
import backend.application.port.out.project.ProjectRepositoryPort;
import backend.application.port.out.task.TaskManagerRepositoryPort;
import backend.application.port.out.task.TaskRepositoryPort;
import backend.application.port.out.user.UserRepositoryPort;
import backend.application.port.out.workspace.WorkspaceMemberRepositoryPort;
import backend.domain.event.impl.TaskUpdatedEvent;
import backend.domain.folder.model.Folder;
import backend.domain.project.model.Project;
import backend.domain.task.model.Task;
import backend.domain.task.model.TaskManager;
import backend.domain.task.model.TaskStatus;
import backend.domain.user.model.User;
import backend.domain.user.model.UserId;
import backend.exception.task.TaskErrorCode;
import backend.exception.task.TaskException;
import backend.exception.user.UserErrorCode;
import backend.exception.user.UserException;
import backend.exception.workspace.WorkspaceErrorCode;
import backend.exception.workspace.WorkspaceException;
import backend.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskService implements TaskUseCase {

    private final TaskRepositoryPort taskRepository;
    private final TaskManagerRepositoryPort taskManagerRepository;
    private final UserRepositoryPort userRepository;
    private final FolderRepositoryPort folderRepository;
    private final WorkspaceMemberRepositoryPort workspaceMemberRepository;
    private final ProjectRepositoryPort projectRepository;
    private final EventPublishingPort eventPublisher;

    @Override
    public Mono<Void> createTask(UpdateTaskCommand command) {
        return getCurrentUser()
                .flatMap(user -> validateWorkspaceAccess(command.workspaceId(), user))
                .flatMap(user -> validateProjectAndWorkspace(command)
                        .thenReturn(user))
                .flatMap(user -> validateParentTask(command.parentId(), command.projectId())
                        .map(validParentIdOpt -> createTaskFromCommand(command, validParentIdOpt))
                        .flatMap(taskRepository::save)
                        .flatMap(savedTask -> saveTaskManagers(command.managerIds(), savedTask, user)));
    }
    @Override
    public Mono<Void> updateTask(Long taskId, UpdateTaskCommand command) {
        return getCurrentUser()
                .flatMap(user -> validateWorkspaceAccess(command.workspaceId(), user))
                .flatMap(user -> taskRepository.findById(taskId)
                        .switchIfEmpty(Mono.error(new TaskException(TaskErrorCode.TASK_NOT_FOUND)))
                        .flatMap(previousTask -> validateUpdateCommand(command)
                                .thenReturn(previousTask))
                        .flatMap(previousTask -> processTaskUpdate(previousTask, command, user))
                )
                .then();
    }


    //폴더와 워크스페이스 검증
    private Mono<UpdateTaskCommand> validateProjectAndWorkspace(UpdateTaskCommand command) {
        return getProject(command.projectId())
                .flatMap(project -> getFolder(project.getFolderId())
                        .flatMap(folder -> {
                            if (!folder.getWorkspaceId().equals(command.workspaceId())) {
                                return Mono.error(new WorkspaceException(WorkspaceErrorCode.FOLDER_NOT_IN_WORKSPACE));
                            }
                            return Mono.just(command);
                        })
                );
    }

    private Mono<Project> getProject(Long projectId) {
        return projectRepository.findById(projectId)
                .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.PROJECT_NOT_FOUND)));
    }

    private Mono<Optional<Long>> validateParentTask(Long parentId, Long projectId) {
        // parentId가 null이거나 0이면 root 태스크로 처리
        if (parentId == null || parentId == 0) {
            return Mono.just(Optional.empty());
        }

        // 부모 태스크가 존재하는지 확인
        return getParentTaskOptional(parentId)
                .flatMap(parentTaskOpt -> {
                    if (parentTaskOpt.isPresent()) {
                        Task parentTask = parentTaskOpt.get();
                        // 부모와 자식는 같은 프로젝트에 속해야함
                        if (!parentTask.getProjectId().equals(projectId)) {
                            return Mono.error(new WorkspaceException(WorkspaceErrorCode.TASK_NOT_IN_PROJECT));
                        }
                        return Mono.just(Optional.of(parentTask.getId().getValue()));
                    } else {
                        return Mono.just(Optional.empty());
                    }
                });
    }

    private Mono<Optional<Task>> getParentTaskOptional(Long parentId) {
        return taskRepository.findById(parentId)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());
    }

    private Mono<Boolean> isWorkspaceMember(Long workspaceId, User user) {
        return workspaceMemberRepository.existsByUserIdAndWorkspaceId(user.getId().getValue(), workspaceId)
                .defaultIfEmpty(false);
    }

    private Mono<Folder> getFolder(Long folderId) {
        return folderRepository.findById(folderId)
                .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.FOLDER_NOT_FOUND)));
    }

    private Mono<User> getCurrentUser() {
        return SecurityUtils.getCurrentUserId()
                .flatMap(userId -> {
                    return userRepository.findById(UserId.of(userId))
                            .switchIfEmpty(Mono.error(new UserException(UserErrorCode.USER_NOT_FOUND)));
                });
    }

    private Mono<User> validateWorkspaceAccess(Long workspaceId, User user) {
        return workspaceMemberRepository.existsByUserIdAndWorkspaceId(user.getId().getValue(), workspaceId)
                .flatMap(isMember -> {
                    if (!isMember) {
                        return Mono.error(new WorkspaceException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));
                    }
                    return Mono.just(user);
                });
    }
    private Task createTaskFromCommand(UpdateTaskCommand command, Optional<Long> parentId) {
        return Task.builder()
                .projectId(command.projectId())
                .parentId(parentId.orElse(null))
                .taskName(command.taskName())
                .taskStatus(TaskStatus.fromString(command.taskStatus()))
                .startDate(command.startDate())
                .endDate(command.endDate())
                .description(command.description())
                .fileUrl(command.fileUrl())
                .build();
    }
    private Mono<Void> saveTaskManagers(List<Long> managerIds, Task savedTask, User currentUser) {
        List<Long> finalManagerIds = Optional.ofNullable(managerIds)
                .filter(list -> !list.isEmpty())
                .orElse(List.of(currentUser.getId().getValue()));

        return Flux.fromIterable(finalManagerIds)
                .distinct()
                .flatMap(managerId -> createAndSaveTaskManager(savedTask.getId().getValue(), managerId))
                .then(publishTaskUpdatedEvent(savedTask));
    }

    private Mono<TaskManager> createAndSaveTaskManager(Long taskId, Long managerId) {
        TaskManager taskManager = TaskManager.builder()
                .taskId(taskId)
                .userId(managerId)
                .build();
        return taskManagerRepository.save(taskManager);
    }
    private Mono<Void> validateUpdateCommand(UpdateTaskCommand command) {
        return validateProjectAndWorkspace(command)
                .then(validateParentTask(command.parentId(), command.projectId()))
                .then();
    }
    private Mono<Void> publishTaskUpdatedEvent(Task savedTask) {
        // 변경 사항 여부 검사 없이 이벤트 발행 (알림은 지연돼도 무방하지만, 상태 변경은 즉시 반영되어야 함)
        TaskUpdatedEvent event = TaskUpdatedEvent.builder()
                .param(savedTask)
                .build();
        return eventPublisher.publishEvent(event);
    }
    private Mono<Void> processTaskUpdate(Task previousTask, UpdateTaskCommand command, User currentUser) {
        return validateUpdateCommand(command)
                .then(Mono.fromCallable(() -> previousTask.updateWith(command, previousTask)))
                .flatMap(taskRepository::save)
                .flatMap(savedTask -> updateTaskManagers(savedTask, command.managerIds(), currentUser).thenReturn(savedTask))
                .flatMap(this::publishTaskUpdatedEvent);
    }
    private Mono<Void> updateTaskManagers(Task savedTask, List<Long> managerIds, User currentUser) {
        return taskManagerRepository.deleteByTaskId(savedTask.getId().getValue())
                .then(saveTaskManagers(managerIds, savedTask, currentUser))
                .then();
    }
    @Override
    public Mono<TaskDetailResult> getTaskDetail(Long taskId) {
        return null;
    }
}