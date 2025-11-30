package backend.infrastructure.adapter.out.persistence.db.r2dbc.folder;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface FolderR2dbcRepository extends R2dbcRepository<FolderEntity, Long> {
    Mono<FolderEntity> findById(Long id);

    Flux<FolderEntity> findAllByWorkspaceId(Long workspaceId);
}