package backend.application.port.out.workspace;

import backend.domain.workspace.model.Workspace;
import reactor.core.publisher.Mono;

public interface WorkspaceRepositoryPort {
    Mono<Workspace> save(Workspace workspace);

    Mono<Workspace> findById(Long id);

}
