package backend.infrastructure.adapter.in.web.rest.auth;

import backend.application.port.in.auth.AuthUseCase;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Auth 외부 요청을 받는 진입 지점
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthUseCase authUseCase;

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        var command = new AuthUseCase.LoginCommand(
                request.email(), request.password(), request.rememberMe()
        );
        return authUseCase.login(command)
                .map(result -> {
                    ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", result.refreshToken())
                            .httpOnly(true).secure(true).sameSite("Strict")
                            .maxAge(result.refreshTokenExpiration() / 1000)
                            .path("/").build();
                    LoginResponse response = new LoginResponse(
                            result.accessToken(), result.userId(), result.email(), result.nickname()
                    );
                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                            .body(response);
                });
    }

    @Operation(summary = "Token 재발급")
    @PostMapping("/refresh")
    public Mono<ResponseEntity<RefreshResponse>> refresh(ServerWebExchange exchange) {
        String refreshToken = extractRefreshTokenFromCookie(exchange);
        if (refreshToken == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        return authUseCase.refresh(new AuthUseCase.RefreshCommand(refreshToken))
                .map(result -> {
                    var cookie = ResponseCookie.from("refreshToken", result.refreshToken())
                            .httpOnly(true).secure(true).sameSite("Strict")
                            .maxAge(result.refreshTokenExpiration() / 1000)
                            .path("/").build();
                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, cookie.toString())
                            .body(new RefreshResponse(result.accessToken()));
                })
                .onErrorReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(ServerWebExchange exchange) {
        String refreshToken = extractRefreshTokenFromCookie(exchange);

        ResponseCookie expiredCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .maxAge(0)
                .path("/")
                .build();

        if (refreshToken != null) {
            return authUseCase.logout(new AuthUseCase.LogoutCommand(refreshToken))
                    .then(Mono.just(ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, expiredCookie.toString()).build()));
        }
        return Mono.just(ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, expiredCookie.toString()).build());
    }

    @PostMapping("/verification-codes")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "이메일 인증 코드 전송")
    public Mono<Void> sendVerificationCode(@Valid @RequestBody VerificationCodeRequest request) {
        return authUseCase.sendVerificationCode(new AuthUseCase.SendVerificationCodeCommand(request.email()))
                .then();
    }

    @PostMapping("/magic-link")
    @Operation(summary = "매직 링크 전송")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> sendMagicLink(@Valid @RequestBody MagicLinkRequest request) {
        AuthUseCase.SendMagicLinkCommand command = new AuthUseCase.SendMagicLinkCommand(request.email());
        return authUseCase.sendMagicLink(command)
                .then();
    }

    @Operation(summary = "매직 링크 검증 및 로그인")
    @PostMapping("/magic-links/verification")
    public Mono<ResponseEntity<LoginResponse>> verifyMagicLink(@RequestParam String token) {
        AuthUseCase.VerifyMagicLinkCommand command = new AuthUseCase.VerifyMagicLinkCommand(token);

        return authUseCase.verifyMagicLink(command)
                .map(result -> {
                    ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", result.refreshToken())
                            .httpOnly(true)
                            .secure(true)
                            .sameSite("Strict")
                            .maxAge(result.refreshTokenExpiration() / 1000)
                            .path("/")
                            .build();

                    LoginResponse response = new LoginResponse(
                            result.accessToken(),
                            result.userId(),
                            result.email(),
                            result.nickname()
                    );

                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                            .body(response);
                });
    }

    private String extractRefreshTokenFromCookie(ServerWebExchange exchange) {
        return exchange.getRequest().getCookies().getFirst("refreshToken").getValue();
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            boolean rememberMe
    ) {}

    public record LoginResponse(
            String accessToken,
            String userId,
            String email,
            String nickname
    ) {}

    public record RefreshResponse(String accessToken) {}
    public record MagicLinkRequest(@NotBlank @Email String email) {}
    public record VerificationCodeRequest(@NotBlank @Email String email) {}
}
