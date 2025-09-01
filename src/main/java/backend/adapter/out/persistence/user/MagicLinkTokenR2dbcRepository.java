package backend.adapter.out.persistence.user;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface MagicLinkTokenR2dbcRepository extends R2dbcRepository<MagicLinkTokenEntity, UUID> {
    Mono<MagicLinkTokenEntity> findByToken(String token);
    Mono<Void> deleteByEmail(String email);
}
