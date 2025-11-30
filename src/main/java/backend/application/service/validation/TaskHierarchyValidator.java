package backend.application.service.validation;

import backend.application.port.in.task.TaskUseCase;
import backend.application.port.out.folder.FolderRepositoryPort;
import backend.application.port.out.project.ProjectRepositoryPort;
import backend.application.port.out.task.TaskRepositoryPort;
import backend.domain.task.dto.request.UpdateTaskRequest;
import backend.exception.folder.FolderErrorCode;
import backend.exception.folder.FolderException;
import backend.exception.project.ProjectErrorCode;
import backend.exception.project.ProjectException;
import backend.exception.task.TaskErrorCode;
import backend.exception.task.TaskException;
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

    public Mono<UpdateTaskRequest> validate(UpdateTaskRequest request) {
        return Mono.when(
                validateProjectHierarchy(request),
                validateParentTaskConsistency(request)
        ).thenReturn(request);
    }

    private Mono<Void> validateProjectHierarchy(UpdateTaskRequest request) {
        return projectRepository.findById(request.projectId())
                .switchIfEmpty(Mono.error(new ProjectException(ProjectErrorCode.PROJECT_NOT_FOUND)))
                .flatMap(project -> folderRepository.findById(project.getFolderId()))
                .switchIfEmpty(Mono.error(new FolderException(FolderErrorCode.FOLDER_NOT_FOUND)))
                .filter(folder -> folder.getWorkspaceId().equals(request.workspaceId()))
                .switchIfEmpty(Mono.error(new WorkspaceException(FolderErrorCode.FOLDER_NOT_IN_WORKSPACE)))
                .then();
    }

    private Mono<Void> validateParentTaskConsistency(UpdateTaskRequest request) {
        if (request.parentId() == 0) return Mono.empty();

        return taskRepository.findById(request.parentId())
                .switchIfEmpty(Mono.error(new TaskException(TaskErrorCode.TASK_NOT_FOUND)))
                .filter(parentTask -> parentTask.getProjectId().equals(request.projectId()))
                .switchIfEmpty(Mono.error(new ProjectException(ProjectErrorCode.PARENT_TASK_DIFFERENT_PROJECT)))
                .then();
    }
}
