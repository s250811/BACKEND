package backend.infrastructure.adapter.out.auth;

import backend.application.port.out.auth.MagicLinkPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MagicLinkAdapter implements MagicLinkPort {

    private static final String MAGIC_LINK_EMAIL_PREFIX = "magic_link:email:";
    private static final String MAGIC_LINK_TOKEN_PREFIX = "magic_link:token:";
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Override
    public String generateMagicLinkToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public Mono<Void> storeMagicLinkToken(String email, String token, long expirationMs) {
        Duration ttl = Duration.ofMillis(expirationMs);

        String emailKey = MAGIC_LINK_EMAIL_PREFIX + email;
        String tokenKey = MAGIC_LINK_TOKEN_PREFIX + token;

        return reactiveRedisTemplate.opsForValue().set(emailKey, token, ttl)
                .then(reactiveRedisTemplate.opsForValue().set(tokenKey, email, ttl))
                .then();
    }

    @Override
    public Mono<String> getMagicLinkToken(String email) {
        String key = MAGIC_LINK_EMAIL_PREFIX + email;
        return reactiveRedisTemplate.opsForValue().get(key);
    }

    @Override
    public Mono<Void> deleteMagicLinkToken(String email) {
        return getMagicLinkToken(email)
                .flatMap(token -> {
                    String emailKey = MAGIC_LINK_EMAIL_PREFIX + email;
                    String tokenKey = MAGIC_LINK_TOKEN_PREFIX + token;
                    return reactiveRedisTemplate.delete(emailKey)
                            .then(reactiveRedisTemplate.delete(tokenKey));
                })
                .then();
    }

    public Mono<String> findEmailByToken(String token) {
        String key = MAGIC_LINK_TOKEN_PREFIX + token;
        return reactiveRedisTemplate.opsForValue().get(key);
    }
}
