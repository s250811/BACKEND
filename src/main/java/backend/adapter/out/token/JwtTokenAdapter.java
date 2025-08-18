package backend.adapter.out.token;

import backend.application.port.out.TokenServicePort;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 토큰 생성 및 관리 기능 out port 구현체 (port → jpaRepository 호출)
 * JWT 토큰을 생성하고, 인증 코드 및 매직 링크를 생성합니다.
 */
@Slf4j
@Component
public class JwtTokenAdapter implements TokenServicePort {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;

    public JwtTokenAdapter(@Value("${jwt.secret}") String secret,
                           @Value("${jwt.access-token-expiration}") long accessTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpiration = accessTokenExpiration;
    }

    @Override
    public String generateAccessToken(String userId, String email) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .setSubject(userId)
                .claim("email", email)
                .claim("type", "access")
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    @Override
    public String generateMagicLinkToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * 6자리 인증코드 생성
     */
    @Override
    public String generateVerificationCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }
}
