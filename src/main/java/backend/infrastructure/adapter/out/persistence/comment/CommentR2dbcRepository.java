package backend.infrastructure.adapter.out.persistence.comment;

import io.micrometer.observation.ObservationFilter;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Repository
public interface CommentR2dbcRepository extends R2dbcRepository<CommentEntity, Long> {
    Flux<CommentEntity> findByTaskId(Long value);
}

