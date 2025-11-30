package backend.infrastructure.adapter.out.persistence.db.r2dbc.workspace;

import backend.application.port.out.workspace.WorkspaceRepositoryPort;
import backend.domain.workspace.model.Workspace;
import backend.domain.workspace.model.WorkspaceId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class WorkPersistenceAdapter implements WorkspaceRepositoryPort {

    private final WorkspaceR2dbcRepository repository;

    @Override
    public Mono<Workspace> save(Workspace workspace) {
        WorkspaceEntity entity = toEntity(workspace);
        return repository.save(entity)
                .map(WorkPersistenceAdapter::toDomain);
    }

    @Override
    public Mono<Workspace> findById(Long id) {
        return repository.findById(id)
                .map(WorkPersistenceAdapter::toDomain);
    }

    @Override
    public Mono<Boolean> existsById(Long id) {
        return repository.existsById(id);
    }

    private static WorkspaceEntity toEntity(Workspace workspace) {
        return WorkspaceEntity.builder()
                .id(workspace.getIdValue())
                .workspaceName(workspace.getWorkspaceName())
                .workspaceImgUrl(workspace.getWorkspaceImgUrl())
                .description(workspace.getDescription())
                .isDeleted(workspace.isDeleted())
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .build();
    }

    private static Workspace toDomain(WorkspaceEntity entity) {
        return Workspace.builder()
                .id(WorkspaceId.of(entity.getId()))
                .workspaceName(entity.getWorkspaceName())
                .workspaceImgUrl(entity.getWorkspaceImgUrl())
                .description(entity.getDescription())
                .isDeleted(entity.isDeleted())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}