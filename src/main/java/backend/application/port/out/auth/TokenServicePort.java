package backend.application.port.out.auth;

import reactor.core.publisher.Mono;

public interface TokenServicePort {
    String generateAccessToken(String userId);
    String generateRefreshToken(String userId);
    boolean validateToken(String token);
    String getUserIdFromToken(String token);
    Mono<Void> storeRefreshToken(String userId, String refreshToken);
    Mono<Boolean> validateRefreshToken(String userId, String refreshToken);
    Mono<Void> deleteRefreshToken(String userId);

    Long getUserIdAsLongFromToken(String token);
}
