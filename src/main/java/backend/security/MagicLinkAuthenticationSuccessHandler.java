package backend.security;

import backend.application.port.in.AuthUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MagicLinkAuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {

    private final AuthUseCase authUseCase;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        String email = authentication.getName();
        AuthUseCase.MagicLinkLoginCommand command = new AuthUseCase.MagicLinkLoginCommand(email);

        return authUseCase.login(command)
                .flatMap(result -> {
                    ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", result.refreshToken())
                            .httpOnly(true).secure(true).sameSite("Strict")
                            .maxAge(result.refreshTokenExpiration() / 1000)
                            .path("/").build();
                    Map<String, Object> responseBody = Map.of(
                            "accessToken", result.accessToken(),
                            "userId", result.userId(),
                            "email", result.email(),
                            "nickname", result.nickname());
                    try {
                        String jsonResponse = objectMapper.writeValueAsString(responseBody);
                        DataBuffer buffer = webFilterExchange.getExchange().getResponse().bufferFactory()
                                .wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
                        var response = webFilterExchange.getExchange().getResponse();
                        response.setStatusCode(HttpStatus.OK);
                        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                        response.getHeaders().add(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

                        return response.writeWith(Mono.just(buffer));
                    } catch (Exception e) {
                        log.error("매직링크 응답 생성 실패", e);
                        webFilterExchange.getExchange().getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                        return webFilterExchange.getExchange().getResponse().setComplete();
                    }
                })
                .doOnSuccess(unused -> log.info("매직링크 인증 성공: {}", email))
                .onErrorResume(error -> {
                    log.error("매직링크 인증 처리 실패: {}", email, error);
                    webFilterExchange.getExchange().getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return webFilterExchange.getExchange().getResponse().setComplete();
                });
    }
}