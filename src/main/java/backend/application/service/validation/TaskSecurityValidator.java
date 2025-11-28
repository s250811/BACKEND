package backend.application.service.validation;

import backend.application.port.out.workspace.WorkspaceMemberRepositoryPort;
import backend.domain.task.dto.request.UpdateTaskRequest;
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

    public Mono<UpdateTaskRequest> validate(UpdateTaskRequest request) {
        return SecurityUtils.getCurrentUserId()
                .flatMap(userId -> validateWorkspaceAccess(userId, request.workspaceId()))
                .thenReturn(request);
    }

    private Mono<Void> validateWorkspaceAccess(Long userId, Long workspaceId) {
        return workspaceMemberRepository.existsByUserIdAndWorkspaceId(userId, workspaceId)
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED)))
                .then();
    }
}