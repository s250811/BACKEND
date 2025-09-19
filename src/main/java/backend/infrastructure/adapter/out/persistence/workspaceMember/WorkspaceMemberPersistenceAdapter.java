package backend.infrastructure.adapter.out.persistence.workspaceMember;

import backend.application.port.out.workspace.WorkspaceMemberRepositoryPort;
import backend.domain.user.model.UserId;
import backend.domain.workspace.model.WorkspaceId;
import backend.domain.workspaceMember.model.WorkspaceMember;
import backend.domain.workspaceMember.model.WorkspaceMemberId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class WorkspaceMemberPersistenceAdapter implements WorkspaceMemberRepositoryPort {

    private final WorkspaceMemberR2dbcRepository repository;

    @Override
    public Mono<WorkspaceMember> findByUserId(Long userId) {
        return repository.findByUserId(userId)
                .map(WorkspaceMemberPersistenceAdapter::toDomain);
    }

    @Override
    public Mono<WorkspaceMember> save(WorkspaceMember workspaceMember) {
        WorkspaceMemberEntity entity = toEntity(workspaceMember);
        return repository.save(entity)
                .map(WorkspaceMemberPersistenceAdapter::toDomain);
    }

    @Override
    public Mono<Boolean> existsByUserIdAndWorkspaceId(Long userId, Long workspaceId) {
        return repository.existsByUserIdAndWorkspaceId(userId, workspaceId);
    }

    @Override
    public Flux<WorkspaceMember> findAllByWorkspaceId(Long workspaceId) {
        return repository.findAllByWorkspaceId(workspaceId)
                .map(WorkspaceMemberPersistenceAdapter::toDomain);
    }


    @Override
    public Mono<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId) {
        return this.findAllByWorkspaceId(workspaceId)
                .filter(domain -> domain.getUserId().equals(userId))
                .next();
    }


    private static WorkspaceMember toDomain(WorkspaceMemberEntity entity) {
        return WorkspaceMember.builder()
                .id(WorkspaceMemberId.of(entity.getId()))
                .userId(entity.getUserId())
                .nickname(entity.getNickname())
                .workspaceId(WorkspaceId.of(entity.getWorkspaceId()))
                .role(entity.getRole())
                .isDeleted(entity.isDeleted())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private WorkspaceMemberEntity toEntity(WorkspaceMember workspaceMember) {
        return WorkspaceMemberEntity.builder()
                .id(workspaceMember.getIdValue())
                .userId(workspaceMember.getUserId())
                .nickname(workspaceMember.getNickname())
                .workspaceId(workspaceMember.getWorkspaceId().getValue())
                .role(workspaceMember.getRole())
                .isDeleted(workspaceMember.isDeleted())
                .createdAt(workspaceMember.getCreatedAt())
                .updatedAt(workspaceMember.getUpdatedAt())
                .build();
    }


}
