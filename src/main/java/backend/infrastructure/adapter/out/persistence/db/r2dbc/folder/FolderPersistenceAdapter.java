package backend.infrastructure.adapter.out.persistence.db.r2dbc.folder;

import backend.application.port.out.folder.FolderRepositoryPort;
import backend.domain.folder.model.Folder;
import backend.domain.folder.model.FolderId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class FolderPersistenceAdapter implements FolderRepositoryPort {

    private final FolderR2dbcRepository repository;

    @Override
    public Mono<Folder> save(Folder folder) {
        FolderEntity entity = toEntity(folder);
        return repository.save(entity)
                .map(FolderPersistenceAdapter::toDomain);
    }

    @Override
    public Mono<Folder> findById(Long id) {
        return repository.findById(id)
                .map(FolderPersistenceAdapter::toDomain);
    }

    @Override
    public Flux<Folder> findAllByWorkspaceId(Long workspaceId) {
        return repository.findAllByWorkspaceId(workspaceId)
                .map(FolderPersistenceAdapter::toDomain);
    }

    private static FolderEntity toEntity(Folder folder) {
        return FolderEntity.builder()
                .id(folder.getIdValue())
                .workspaceId(folder.getWorkspaceId())
                .folderName(folder.getFolderName())
                .isDeleted(folder.isDeleted())
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }

    private static Folder toDomain(FolderEntity entity) {
        return Folder.builder()
                .id(FolderId.of(entity.getId()))
                .workspaceId(entity.getWorkspaceId())
                .folderName(entity.getFolderName())
                .isDeleted(entity.isDeleted())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
