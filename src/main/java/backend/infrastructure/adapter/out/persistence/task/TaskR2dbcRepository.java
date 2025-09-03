package backend.infrastructure.adapter.out.persistence.task;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface TaskR2dbcRepository extends R2dbcRepository<TaskEntity, Long> {
    Mono<TaskEntity> findById(Long id);

    Flux<TaskEntity> findAllByProjectIdIn(List<Long> projectIds);
}
