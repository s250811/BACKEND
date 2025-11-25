package backend.application.service.validation;

import backend.application.port.in.workspace.WorkspaceUseCase.CreateWorkspaceCommand;
import backend.application.port.out.workspace.WorkspaceMemberRepositoryPort;
import backend.application.port.out.workspace.WorkspaceRepositoryPort;
import backend.domain.workspaceMember.model.WorkspaceMemberRole;
import backend.exception.workspace.WorkspaceErrorCode;
import backend.exception.workspace.WorkspaceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class WorkspaceBusinessValidator {

    private final WorkspaceRepositoryPort workspaceRepository;
    private final WorkspaceMemberRepositoryPort workspaceMemberRepository;

    public Mono<CreateWorkspaceCommand> validateCreateWorkspace(CreateWorkspaceCommand command, Long userId) {
        return validateUserCanCreateWorkspace(userId)
                .thenReturn(command);
    }
    /**
     * 사용자가 워크스페이스를 생성할 수 있는지 검증
     * (이미 워크스페이스 소유자인 경우 생성 불가)
     */
    private Mono<Void> validateUserCanCreateWorkspace(Long userId) {
        return workspaceMemberRepository.findByUserId(userId)
                .filter(member -> member.getRole() == WorkspaceMemberRole.OWNER)
                .hasElement()
                .flatMap(hasOwnership -> {
                    if (hasOwnership) {
                        return Mono.error(new WorkspaceException(WorkspaceErrorCode.USER_ALREADY_OWNS_WORKSPACE));
                    }
                    return Mono.empty();
                });
    }

    public Mono<Void> validateWorkspaceExists(Long workspaceId) {
        return workspaceRepository.existsById(workspaceId)
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND)))
                .then();
    }
}