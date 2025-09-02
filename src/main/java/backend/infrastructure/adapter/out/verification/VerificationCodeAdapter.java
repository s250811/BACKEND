package backend.infrastructure.adapter.out.verification;

import backend.application.port.out.VerificationCodePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class VerificationCodeAdapter implements VerificationCodePort {
    private static final String VERIFICATION_CODE_PREFIX = "verification_code:";
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Override
    public String generateVerificationCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }

    @Override
    public Mono<Void> storeVerificationCode(String email, String code, long expirationMs) {
        String key = VERIFICATION_CODE_PREFIX + email;
        Duration ttl = Duration.ofMillis(expirationMs);
        return reactiveRedisTemplate.opsForValue().set(key, code, ttl).then();
    }

    @Override
    public Mono<String> getVerificationCode(String email) {
        String key = VERIFICATION_CODE_PREFIX + email;
        return reactiveRedisTemplate.opsForValue().get(key);
    }

    @Override
    public Mono<Void> deleteVerificationCode(String email) {
        String key = VERIFICATION_CODE_PREFIX + email;
        return reactiveRedisTemplate.delete(key).then();
    }
}
