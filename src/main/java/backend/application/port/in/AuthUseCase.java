package backend.application.port.in;

import reactor.core.publisher.Mono;

public interface AuthUseCase {

    record LoginCommand(String email, String password, boolean rememberMe) {}
    record MagicLinkLoginCommand(String email) {}
    record LoginResult(
            String accessToken,
            String refreshToken,
            long refreshTokenExpiration,
            String userId,
            String email,
            String nickname
    ) {}
    record RefreshCommand(String refreshToken) {}
    record RefreshResult(String accessToken, String refreshToken, long refreshTokenExpiration) {}
    record LogoutCommand(String refreshToken) {}
    record SendVerificationCodeCommand(String email){}
    record SendVerificationCodeResult(String code){}

    Mono<LoginResult> login(LoginCommand command);
    Mono<LoginResult> login(MagicLinkLoginCommand command);
    Mono<RefreshResult> refresh(RefreshCommand command);
    Mono<Void> logout(LogoutCommand command);
    Mono<SendVerificationCodeResult> sendVerificationCode(SendVerificationCodeCommand command);
}
