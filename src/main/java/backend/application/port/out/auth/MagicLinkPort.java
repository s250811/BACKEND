package backend.application.port.out.auth;

import reactor.core.publisher.Mono;

public interface MagicLinkPort {
    String generateMagicLinkToken();
    Mono<Void> storeMagicLinkToken(String email, String token, long expirationMs);
    Mono<String> getMagicLinkToken(String email);
    Mono<Void> deleteMagicLinkToken(String email);
    Mono<String> findEmailByToken(String token);
}
