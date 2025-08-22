package backend.application.port.out;

import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;

public interface TokenServicePort {
    String generateAccessToken(String userId, String email);
    String generateRefreshToken(String userId);
    String generateMagicLinkToken();
    String generateVerificationCode();
    boolean validateToken(String token);
    Claims getClaimsFromToken(String token);
    String getUserIdFromToken(String token);
    String getEmailFromToken(String token);
    Mono<Void> storeRefreshToken(String userId, String refreshToken);
    Mono<Boolean> validateRefreshToken(String userId, String refreshToken);
    Mono<Void> deleteRefreshToken(String userId);
    Mono<Void> storeVerificationCode(String email, String code, long expirationMs);
    Mono<String> getVerificationCode(String email);
    Mono<Void> deleteVerificationCode(String email);
    String generateRandomNickname();
}
