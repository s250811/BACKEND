package backend.security;

import backend.application.port.out.EmailServicePort;
import backend.application.port.out.UserRepositoryPort;
import backend.domain.user.model.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.web.server.authentication.ott.ServerOneTimeTokenGenerationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class MagicLinkTokenGenerationSuccessHandler implements ServerOneTimeTokenGenerationSuccessHandler {

    private final UserRepositoryPort userRepository;
    private final EmailServicePort emailService;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, OneTimeToken oneTimeToken) {
        String email = oneTimeToken.getUsername();

        return userRepository.existsByEmail(new Email(email))
                .filter(exists -> exists)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("존재하지 않는 사용자입니다.")))
                .then(Mono.defer(() -> {
                    String magicLink = "http://localhost:3000/auth/magic-login?token=" + oneTimeToken.getTokenValue();
                    return emailService.sendMagicLinkEmail(email, magicLink);
                }))
                .doOnSuccess(unused -> {
                    log.info("매직링크 전송 완료: {}", email);
                    exchange.getResponse().setStatusCode(HttpStatus.OK);
                })
                .doOnError(error -> {
                    log.error("매직링크 전송 실패: {}, error: {}", email, error.getMessage());
                    if (error instanceof IllegalArgumentException) {
                        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                    } else {
                        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                })
                .then(exchange.getResponse().setComplete())
                .onErrorResume(error -> exchange.getResponse().setComplete());
    }
}

