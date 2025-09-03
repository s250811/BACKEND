package backend.infrastructure.security;

import backend.application.port.out.auth.TokenServicePort;
import backend.application.port.out.user.UserRepositoryPort;
import backend.domain.user.model.UserId;
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
    private final UserRepositoryPort userRepository;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = authentication.getCredentials().toString();

        if (!tokenService.validateToken(token)) {
            return Mono.empty();
        }

        String userId = tokenService.getUserIdFromToken(token);

        return userRepository.findById(UserId.of(Long.valueOf(userId)))
                .map(user -> new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(user.getId().getValue().toString(), user.getEmail().getValue()),
                        token,
                        Collections.singletonList(new SimpleGrantedAuthority("MEMBER"))
                ));
    }

    public record AuthenticatedUser(String userId, String email) {}
}