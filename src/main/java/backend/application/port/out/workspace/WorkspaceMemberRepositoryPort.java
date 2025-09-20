package backend.application.port.out.workspace;

import backend.domain.workspaceMember.model.WorkspaceMember;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WorkspaceMemberRepositoryPort {

    Mono<WorkspaceMember> findByUserId(Long userId);
    Mono<WorkspaceMember> save(WorkspaceMember workspaceMember);
    Mono<Boolean> existsByUserIdAndWorkspaceId(Long userId, Long workspaceId);
    Flux<WorkspaceMember> findAllByWorkspaceId(Long workspaceId);
    Mono<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);
}
