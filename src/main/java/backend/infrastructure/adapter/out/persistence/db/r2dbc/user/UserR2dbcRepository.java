package backend.infrastructure.adapter.out.persistence.db.r2dbc.user;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;


@Repository
public interface UserR2dbcRepository extends R2dbcRepository<UserEntity, Long> {
    Mono<UserEntity> findByEmail(String email);
    Mono<Boolean> existsByEmail(String email);
}

