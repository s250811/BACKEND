package backend.infrastructure.adapter.out.persistence.db.r2dbc.project;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ProjectR2dbcRepository extends R2dbcRepository<ProjectEntity, Long> {
    Mono<ProjectEntity> findByFolderId(Long folderId);

    Flux<ProjectEntity> findAllByFolderIdIn(List<Long> folderIds);

    Flux<ProjectEntity> findAllByFolderId(Long folderId);
}
