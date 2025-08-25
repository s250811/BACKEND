package backend.application.port.in;

import reactor.core.publisher.Mono;

public interface AuthUseCase {

    record LoginCommand(String email, String password, boolean rememberMe) {}

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

    record SendMagicLinkCommand(String email) {}

    record SendMagicLinkResult(String msg) {}

    record VerifyMagicLinkCommand(String token) {}

    record VerifyMagicLinkResult(
            String accessToken,
            String refreshToken,
            long refreshTokenExpiration,
            String userId,
            String email,
            String nickname
    ) {}

    record SendVerificationCodeCommand(String email){}
    record SendVerificationCodeResult(String code){}

    Mono<LoginResult> login(LoginCommand command);
    Mono<RefreshResult> refresh(RefreshCommand command);
    Mono<Void> logout(LogoutCommand command);
    Mono<Void> sendMagicLink(SendMagicLinkCommand command);
    Mono<VerifyMagicLinkResult> verifyMagicLink(VerifyMagicLinkCommand command);
    Mono<SendVerificationCodeResult> sendVerificationCode(SendVerificationCodeCommand command);
}
