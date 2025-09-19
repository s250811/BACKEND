package backend.application.service.validation;

import backend.application.port.out.workspace.WorkspaceMemberRepositoryPort;
import backend.domain.workspaceMember.model.WorkspaceMemberRole;
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
public class WorkspaceSecurityValidator {

    private final WorkspaceMemberRepositoryPort workspaceMemberRepository;

    public Mono<Long> validateWorkspaceAccess(Long workspaceId) {
        return SecurityUtils.getCurrentUserId()
                .switchIfEmpty(Mono.error(new UserException(UserErrorCode.USER_NOT_FOUND)))
                .flatMap(userId ->
                        workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                                .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER)))
                                .thenReturn(workspaceId)
                );
    }
}