package backend.infrastructure.security;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

public class SecurityUtils {

    public static Mono<String> getCurrentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication().getPrincipal())
                .cast(JwtAuthenticationManager.AuthenticatedUser.class)
                .map(JwtAuthenticationManager.AuthenticatedUser::userId);
    }
}
