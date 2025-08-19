package backend.application.port.in;

import reactor.core.publisher.Mono;

public interface UserUseCase {

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

    record VerifyEmailCommand(
            String email,
            String code
    ) {}

    record VerifyEmailResult(
            String message,
            boolean verified
    ) {}

    Mono<RegisterUserResult> register(RegisterUserCommand command);
    Mono<VerifyEmailResult> verifyEmail(VerifyEmailCommand command);
}
