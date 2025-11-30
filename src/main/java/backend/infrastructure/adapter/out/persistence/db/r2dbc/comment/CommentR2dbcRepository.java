package backend.infrastructure.adapter.out.persistence.db.r2dbc.comment;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;


@Repository
public interface CommentR2dbcRepository extends R2dbcRepository<CommentEntity, Long> {
    Flux<CommentEntity> findByTaskId(Long value);
}

