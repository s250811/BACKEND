package backend.application.port.out.folder;

import backend.domain.folder.model.Folder;
import backend.domain.workspace.model.WorkspaceId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FolderRepositoryPort {
    Mono<Folder> save(Folder folder);
    Mono<Folder> findById(Long id);

    Flux<Folder> findAllByWorkspaceId(WorkspaceId workspaceId);
}
