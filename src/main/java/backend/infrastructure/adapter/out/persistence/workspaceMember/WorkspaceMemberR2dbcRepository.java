package backend.infrastructure.adapter.out.persistence.workspaceMember;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WorkspaceMemberR2dbcRepository extends R2dbcRepository<WorkspaceMemberEntity, Long> {
    Mono<WorkspaceMemberEntity> findByUserId(Long ownerId);

    Mono<Boolean> existsByUserIdAndWorkspaceId(Long userId, Long workspaceId);

    Flux<WorkspaceMemberEntity> findAllByWorkspaceId(Long workspaceId);

}
