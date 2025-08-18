package backend.application.port.in;

import reactor.core.publisher.Mono;

public interface RegisterUserUseCase {

    record RegisterUserCommand(
            String email,
            String password,
            String nickname
    ) {}

    record RegisterUserResult(
            String userId,
            String email,
            String nickname
    ) {}

    Mono<RegisterUserResult> register(RegisterUserCommand command);
}
