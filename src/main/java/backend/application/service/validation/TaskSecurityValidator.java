package backend.application.service.validation;

import backend.application.port.in.TaskUseCase;
import backend.application.port.out.workspace.WorkspaceMemberRepositoryPort;
import backend.exception.user.UserErrorCode;
import backend.exception.user.UserException;
import backend.exception.workspace.WorkspaceErrorCode;
import backend.exception.workspace.WorkspaceException;
import backend.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TaskSecurityValidator {

    private final WorkspaceMemberRepositoryPort workspaceMemberRepository;

    public Mono<TaskUseCase.UpdateTaskCommand> validate(TaskUseCase.UpdateTaskCommand command) {
        return SecurityUtils.getCurrentUserId()
                .switchIfEmpty(Mono.error(new UserException(UserErrorCode.USER_NOT_FOUND)))
                .flatMap(userId -> validateWorkspaceAccess(userId, command.workspaceId()))
                .thenReturn(command);
    }

    private Mono<Void> validateWorkspaceAccess(Long userId, Long workspaceId) {
        return workspaceMemberRepository.existsByUserIdAndWorkspaceId(userId, workspaceId)
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED)))
                .then();
    }
}