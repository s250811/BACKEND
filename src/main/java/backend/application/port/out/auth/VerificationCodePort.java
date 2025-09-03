package backend.application.port.out.auth;

import reactor.core.publisher.Mono;

public interface VerificationCodePort {
    String generateVerificationCode();
    Mono<Void> storeVerificationCode(String email, String code, long expirationMs);
    Mono<String> getVerificationCode(String email);
    Mono<Void> deleteVerificationCode(String email);
}
