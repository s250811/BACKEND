package backend.application.port.out.workspace;

import backend.domain.workspace.model.Workspace;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface WorkspaceRepositoryPort {
    Mono<Workspace> save(Workspace workspace);
    Mono<Workspace> findById(Long id);
    Mono<Boolean> existsById(Long id);
}
