package backend.application.port.out.user;

import backend.domain.user.model.Email;
import backend.domain.user.model.MagicLinkToken;
import reactor.core.publisher.Mono;

public interface MagicLinkTokenRepositoryPort {
    Mono<MagicLinkToken> save(MagicLinkToken token);
    Mono<MagicLinkToken> findByToken(String token);
    Mono<Void> deleteByEmail(Email email);
}
