package backend.infrastructure.security;

import backend.application.port.out.auth.TokenServicePort;
import backend.exception.user.UserErrorCode;
import backend.exception.user.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final TokenServicePort tokenService;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = authentication.getCredentials().toString();

        if (!tokenService.validateToken(token)) {
            throw new UserException(UserErrorCode.INVALID_TOKEN);
        }

        String userId = tokenService.getUserIdFromToken(token);

        return Mono.just(new UsernamePasswordAuthenticationToken(
                userId,
                token,
                Collections.singletonList(new SimpleGrantedAuthority("MEMBER"))
        ));
    }
}