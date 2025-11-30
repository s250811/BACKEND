package backend.application.port.in.auth;

import backend.domain.user.dto.requst.LoginRequest;
import backend.domain.user.dto.response.LoginResponse;
import backend.domain.user.dto.response.RefreshResponse;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

public interface AuthUseCase {
    Mono<Tuple3<LoginResponse,String, Long>> login(LoginRequest command);
    Mono<RefreshResponse> refresh(String refreshToken);
    Mono<Void> logout(String refreshToken);
    Mono<Void> sendMagicLink(String email);
    Mono<Tuple3<LoginResponse,String, Long>> verifyMagicLink(String magicLinkToken);
    Mono<Void> sendVerificationCode(String email);
}
