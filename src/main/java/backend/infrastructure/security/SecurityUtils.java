package backend.infrastructure.security;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SecurityUtils {

    public static Mono<Long> getCurrentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication().getPrincipal())
                .cast(JwtAuthenticationManager.AuthenticatedUser.class)
                .map(JwtAuthenticationManager.AuthenticatedUser::userId);
    }
}
