package backend.adapter.out.persistence.workspace;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface WorkspaceR2dbcRepository extends R2dbcRepository<WorkspaceEntity, Long> {

}
