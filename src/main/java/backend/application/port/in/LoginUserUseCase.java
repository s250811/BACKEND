package backend.application.port.in;

import reactor.core.publisher.Mono;

public interface LoginUserUseCase {

    record LoginCommand(
            String email,
            String password,
            boolean rememberMe
    ) {
    }

    record LoginResult(
            String accessToken,
            String userId,
            String email,
            String nickname
    ) {
    }

    Mono<LoginResult> login(LoginCommand command);
}
