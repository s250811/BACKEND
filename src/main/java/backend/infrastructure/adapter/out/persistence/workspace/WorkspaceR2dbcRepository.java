package backend.infrastructure.adapter.out.persistence.workspace;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface WorkspaceR2dbcRepository extends R2dbcRepository<WorkspaceEntity, Long> {

}
