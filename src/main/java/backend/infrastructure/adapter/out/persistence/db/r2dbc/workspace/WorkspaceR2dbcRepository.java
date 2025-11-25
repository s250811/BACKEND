package backend.infrastructure.adapter.out.persistence.db.r2dbc.workspace;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface WorkspaceR2dbcRepository extends R2dbcRepository<WorkspaceEntity, Long> {

}
