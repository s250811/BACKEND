package backend.adapter.in.web;

import backend.application.port.in.LoginUserUseCase;
import backend.application.port.in.SendMagicLinkUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Auth 외부 요청을 받는 진입 지점
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUserUseCase loginUserUseCase;
    private final SendMagicLinkUseCase sendMagicLinkUseCase;

    @PostMapping("/login")
    public Mono<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginUserUseCase.LoginCommand command =
                new LoginUserUseCase.LoginCommand(
                        request.email(),
                        request.password(),
                        request.rememberMe()
                );

        return loginUserUseCase.login(command)
                .map(result -> new LoginResponse(
                        result.accessToken(),
                        result.userId(),
                        result.email(),
                        result.nickname()
                ));
    }

    @PostMapping("/magic-link")
    @ResponseStatus(HttpStatus.OK)
    public Mono<MagicLinkResponse> sendMagicLink(@Valid @RequestBody MagicLinkRequest request) {
        SendMagicLinkUseCase.SendMagicLinkCommand command = new SendMagicLinkUseCase.SendMagicLinkCommand(request.email());

        return sendMagicLinkUseCase.sendMagicLink(command)
                .map(result -> new MagicLinkResponse(result.message(), result.sent()));
    }

    @GetMapping("/magic-login")
    public Mono<LoginResponse> verifyMagicLink(@RequestParam String token) {
        SendMagicLinkUseCase.VerifyMagicLinkCommand command = new SendMagicLinkUseCase.VerifyMagicLinkCommand(token);

        return sendMagicLinkUseCase.verifyMagicLink(command)
                .map(result -> new LoginResponse(
                        result.accessToken(),
                        result.userId(),
                        result.email(),
                        result.nickname()
                ));
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

    public record MagicLinkRequest(@NotBlank @Email String email) {}

    public record MagicLinkResponse(String message, boolean sent) {}
}
