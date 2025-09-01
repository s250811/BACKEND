package backend.adapter.out.persistence.task;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface TaskManagerR2dbcRepository extends R2dbcRepository<TaskManagerEntity, Long> {
}
