package backend.infrastructure.security;

import backend.exception.user.UserErrorCode;
import backend.exception.user.UserException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SecurityUtils {
    public static Mono<Long> getCurrentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication().getPrincipal())
                .cast(String.class)
                .map(Long::valueOf)
                .onErrorMap(e -> new UserException(UserErrorCode.INVALID_TOKEN));
    }
}
