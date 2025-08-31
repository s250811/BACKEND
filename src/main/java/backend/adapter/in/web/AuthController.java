package backend.adapter.in.web;

import backend.application.port.in.AuthUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.jmx.export.annotation.ManagedOperation;
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
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;

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

    @PostMapping("/magic-links")
    @ManagedOperation(description = "실제 처리는 Spring Security OTT가 담당하며, 이 메서드는 문서화 목적입니다.")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> sendMagicLink(@RequestParam String email) {
        throw new UnsupportedOperationException("Spring Security OTT에 의해 처리됩니다.");
    }

    @PostMapping("/magic-links/verification")
    @ManagedOperation(description = "실제 처리는 Spring Security OTT가 담당하며, 이 메서드는 문서화 목적입니다.")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ResponseEntity<LoginResponse>> validateMagicLink(@RequestParam String token) {
        throw new UnsupportedOperationException("Spring Security OTT에 의해 처리됩니다.");
    }

    @PostMapping("/verification-codes")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ResponseEntity<VerificationCodeResponse>> sendVerificationCode(@Valid @RequestBody VerificationCodeRequest request) {
        return authUseCase.sendVerificationCode(new AuthUseCase.SendVerificationCodeCommand(request.email()))
                .map(result -> {
                    VerificationCodeResponse response = new VerificationCodeResponse(request.email(), result.code());
                    return ResponseEntity.ok(response);
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
    public record VerificationCodeRequest(@NotBlank @Email String email) {}
    public record VerificationCodeResponse(String email, String code) {}
}
