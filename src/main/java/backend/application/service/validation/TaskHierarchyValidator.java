package backend.application.service.validation;

import backend.application.port.in.task.TaskUseCase;
import backend.application.port.out.folder.FolderRepositoryPort;
import backend.application.port.out.project.ProjectRepositoryPort;
import backend.application.port.out.task.TaskRepositoryPort;
import backend.exception.workspace.WorkspaceErrorCode;
import backend.exception.workspace.WorkspaceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TaskHierarchyValidator {

    private final ProjectRepositoryPort projectRepository;
    private final FolderRepositoryPort folderRepository;
    private final TaskRepositoryPort taskRepository;

    public Mono<TaskUseCase.UpdateTaskCommand> validate(TaskUseCase.UpdateTaskCommand command) {
        return Mono.when(
                validateProjectHierarchy(command),
                validateParentTaskConsistency(command)
        ).thenReturn(command);
    }

    private Mono<Void> validateProjectHierarchy(TaskUseCase.UpdateTaskCommand command) {
        return projectRepository.findById(command.projectId())
                .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.PROJECT_NOT_FOUND)))
                .flatMap(project -> folderRepository.findById(project.getFolderId()))
                .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.FOLDER_NOT_FOUND)))
                .filter(folder -> folder.getWorkspaceId().equals(command.workspaceId()))
                .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.FOLDER_NOT_IN_WORKSPACE)))
                .then();
    }

    private Mono<Void> validateParentTaskConsistency(TaskUseCase.UpdateTaskCommand command) {
        if (command.parentId() == 0) return Mono.empty();

        return taskRepository.findById(command.parentId())
                .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.TASK_NOT_FOUND)))
                .filter(parentTask -> parentTask.getProjectId().equals(command.projectId()))
                .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.PARENT_TASK_DIFFERENT_PROJECT)))
                .then();
    }
}
