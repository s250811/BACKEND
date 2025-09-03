package backend.application.service;

import backend.application.port.in.TaskUseCase;
import backend.application.port.out.folder.FolderRepositoryPort;
import backend.application.port.out.project.ProjectRepositoryPort;
import backend.application.port.out.task.TaskManagerRepositoryPort;
import backend.application.port.out.task.TaskRepositoryPort;
import backend.application.port.out.user.UserRepositoryPort;
import backend.application.port.out.workspace.WorkspaceMemberRepositoryPort;
import backend.domain.folder.model.Folder;
import backend.domain.project.model.Project;
import backend.domain.task.model.Task;
import backend.domain.task.model.TaskManager;
import backend.domain.task.model.TaskStatus;
import backend.domain.user.model.User;
import backend.domain.user.model.UserId;
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

    @Override
    public Mono<Void> createTask(CreateTaskCommand command) {
        return getCurrentUser()
                .flatMap(user ->
                        isWorkspaceMember(command.workspaceId(), user)
                                .flatMap(isMember -> {
                                    if (!isMember) {
                                        return Mono.error(new WorkspaceException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));
                                    }
                                    return validateProjectAndWorkspace(command);
                                })
                                .flatMap(validatedCommand ->
                                        validateParentTask(command.parentId(), command.projectId())
                                                .map(validParentIdOpt ->
                                                        Task.builder()
                                                                .projectId(command.projectId())
                                                                .parentId(validParentIdOpt.orElse(null))
                                                                .taskName(command.taskName())
                                                                .taskStatus(TaskStatus.fromString(command.taskStatus()))
                                                                .startDate(command.startDate())
                                                                .endDate(command.endDate())
                                                                .description(command.description())
                                                                .fileUrl(command.fileUrl())
                                                                .build()
                                                )
                                )
                                .flatMap(taskRepository::save)
                                .flatMap(savedTask -> {
                                    List<Long> managerIds = Optional.ofNullable(command.managerIds())
                                            .filter(list -> !list.isEmpty())
                                            .orElse(List.of(user.getId().getValue()));

                                    return Flux.fromIterable(managerIds)
                                            .distinct()
                                            .flatMap(managerId -> {
                                                TaskManager taskManager = TaskManager.builder()
                                                        .taskId(savedTask.getId().getValue())
                                                        .userId(managerId)
                                                        .build();
                                                return taskManagerRepository.save(taskManager);
                                            })
                                            .then(Mono.just(savedTask));
                                })
                )
                .then();
    }

    //폴더와 워크스페이스 검증
    private Mono<CreateTaskCommand> validateProjectAndWorkspace(CreateTaskCommand command) {
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
                .flatMap(userIdStr -> {
                    Long userId = Long.valueOf(userIdStr);
                    return userRepository.findById(UserId.of(userId))
                            .switchIfEmpty(Mono.error(new UserException(UserErrorCode.USER_NOT_FOUND)));
                });
    }

    @Override
    public Mono<TaskDetailResult> getTaskDetail(Long taskId) {
        return null;
    }
}