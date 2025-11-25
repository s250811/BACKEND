package backend.infrastructure.adapter.out.persistence.db.r2dbc.task;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface TaskManagerR2dbcRepository extends R2dbcRepository<TaskManagerEntity, Long> {
    Mono<Void> deleteByTaskId(Long taskId);
}
