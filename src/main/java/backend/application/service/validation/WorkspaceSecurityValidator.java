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
                .flatMap(userId ->
                        workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                                .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER)))
                                .thenReturn(workspaceId)
                );
    }

    public Mono<Void> validateWorkspaceOwner(Long workspaceId, Long userId) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER)))
                .filter(member -> member.getRole() == WorkspaceMemberRole.OWNER)
                .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.WORKSPACE_OWNER_REQUIRED)))
                .then();
    }
}