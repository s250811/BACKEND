package backend.infrastructure.adapter.in.web.rest.auth;

import backend.application.port.in.auth.AuthUseCase;
import backend.domain.user.dto.requst.LoginRequest;
import backend.domain.user.dto.response.LoginResponse;
import backend.infrastructure.adapter.in.web.rest.dto.ApiResponseDto;
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
    public Mono<ResponseEntity<ApiResponseDto<LoginResponse>>> login(@Valid @RequestBody LoginRequest request) {
        return authUseCase.login(request)
                .map(tuple -> {
                    ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", tuple.getT2())
                            .httpOnly(true).secure(true).sameSite("Strict")
                            .maxAge(tuple.getT3())
                            .path("/").build();
                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                            .body(ApiResponseDto.createSuccess(tuple.getT1(), "로그인되었습니다."));
                });
    }

    @Operation(summary = "Token 재발급")
    @PostMapping("/refresh")
    public Mono<ResponseEntity<ApiResponseDto<String>>> refresh(ServerWebExchange exchange) {
        String refreshToken = extractRefreshTokenFromCookie(exchange);
        if (refreshToken == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        return authUseCase.refresh(refreshToken)
                .map(result -> {
                    var cookie = ResponseCookie.from("refreshToken", result.refreshToken())
                            .httpOnly(true).secure(true).sameSite("Strict")
                            .maxAge(result.refreshTokenExpiration())
                            .path("/").build();
                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, cookie.toString())
                            .body(ApiResponseDto.createSuccess(result.accessToken(), "토큰 재발급 성공"));
                })
                .onErrorReturn(ResponseEntity.status(401).body(ApiResponseDto.createError("유효하지 않은 토큰")));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public Mono<ResponseEntity<ApiResponseDto<Void>>> logout(ServerWebExchange exchange) {
        String refreshToken = extractRefreshTokenFromCookie(exchange);

        ResponseCookie expiredCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .maxAge(0)
                .path("/")
                .build();

        if (refreshToken == null) {
            return Mono.just(ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                    .body(ApiResponseDto.createError("로그인이 필요합니다.")));
        }
        return authUseCase.logout(refreshToken)
                .thenReturn(ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                        .body(ApiResponseDto.createSuccessNoContent("로그아웃되었습니다.")));
    }

    @PostMapping("/verification-codes")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "이메일 인증 코드 전송")
    public Mono<ApiResponseDto<Void>> sendVerificationCode(@RequestBody @Email String email) {
        return authUseCase.sendVerificationCode(email)
                .then(Mono.just(ApiResponseDto.createSuccessNoContent("인증 코드가 전송되었습니다.")));
    }

    @PostMapping("/magic-link")
    @Operation(summary = "매직 링크 전송")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponseDto<Void>>sendMagicLink(@RequestBody @Email String email) {
        return authUseCase.sendMagicLink(email)
                .then(Mono.just(ApiResponseDto.createSuccessNoContent("매직 링크가 전송되었습니다.")));
    }

    @Operation(summary = "매직 링크 검증 및 로그인")
    @PostMapping("/magic-links/verification")
    public Mono<ResponseEntity<ApiResponseDto<LoginResponse>>> verifyMagicLink(@RequestParam String token) {
        return authUseCase.verifyMagicLink(token)
                .map(tuple -> {
                    ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tuple.getT2())
                            .httpOnly(true)
                            .secure(true)
                            .sameSite("Strict")
                            .maxAge(tuple.getT3())
                            .path("/")
                            .build();
                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                            .body(ApiResponseDto.createSuccess(tuple.getT1(), "매직 링크 인증 및 로그인 성공"));
                });
    }

    private String extractRefreshTokenFromCookie(ServerWebExchange exchange) {
        return exchange.getRequest().getCookies().getFirst("refreshToken").getValue();
    }
}
