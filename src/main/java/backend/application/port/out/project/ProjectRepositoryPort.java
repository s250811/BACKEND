package backend.application.port.out.project;

import backend.domain.folder.model.FolderId;
import backend.domain.project.model.Project;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ProjectRepositoryPort {
    Mono<Project> save(Project project);
    Mono<Project> findByFolderId(Long folderId);

    Flux<Project> findAllByFolderId(Long folderId);

    Flux<Project> findAllByFolderIdIn(List<FolderId> folderIds);

    Mono<Project> findById(Long projectId);
}
