package backend.adapter.out.token;

import backend.application.port.out.TokenServicePort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 토큰 생성 및 관리 기능 out port 구현체 (port → repository 호출)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenAdapter implements TokenServicePort {

    private static final String REDIS_KEY_PREFIX = "refresh_token:";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String EMAIL_CLAIM = "email";
    private static final String TYPE_CLAIM = "type";
    private static final String VERIFICATION_CODE_PREFIX = "verification_code:";

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private JwtParser getJwtParser() {
        return Jwts.parser()
                .setSigningKey(getSecretKey())
                .build();
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    private String generateToken(String userId, String email, String type, long expiration) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);

        var builder = Jwts.builder()
                .setSubject(userId)
                .claim(TYPE_CLAIM, type)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(getSecretKey());

        if (email != null) {
            builder.claim(EMAIL_CLAIM, email);
        }

        return builder.compact();
    }

    @Override
    public String generateAccessToken(String userId, String email) {
        return generateToken(userId, email, ACCESS_TOKEN_TYPE, accessTokenExpiration);
    }

    @Override
    public String generateRefreshToken(String userId) {
        return generateToken(userId, null, REFRESH_TOKEN_TYPE, refreshTokenExpiration);
    }

    @Override
    public boolean validateToken(String token) {
        try {
            getJwtParser().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Claims getClaimsFromToken(String token) {
        try {
            return getJwtParser()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to parse JWT claims: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid token", e);
        }
    }

    @Override
    public String getUserIdFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    @Override
    public String getEmailFromToken(String token) {
        return getClaimsFromToken(token).get(EMAIL_CLAIM, String.class);
    }

    private String getRedisKey(String userId) {
        return REDIS_KEY_PREFIX + userId;
    }

    @Override
    public Mono<Void> storeRefreshToken(String userId, String refreshToken) {
        String key = getRedisKey(userId);
        Duration ttl = Duration.ofMillis(refreshTokenExpiration);
        return reactiveRedisTemplate.opsForValue().set(key, refreshToken, ttl).then();
    }

    @Override
    public Mono<Boolean> validateRefreshToken(String userId, String refreshToken) {
        String key = getRedisKey(userId);
        return reactiveRedisTemplate.opsForValue().get(key)
                .map(storedToken -> storedToken.equals(refreshToken) && validateToken(refreshToken))
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Void> deleteRefreshToken(String userId) {
        String key = getRedisKey(userId);
        return reactiveRedisTemplate.delete(key).then();
    }

    @Override
    public String generateMagicLinkToken() {
        return UUID.randomUUID().toString();
    }

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
    public String generateRandomNickname() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
