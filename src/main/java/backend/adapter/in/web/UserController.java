package backend.adapter.in.web;

import backend.application.port.in.UserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * User 외부 요청을 받는 진입 지점
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserUseCase userUseCase;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        UserUseCase.RegisterUserCommand command =
                new UserUseCase.RegisterUserCommand(
                        request.email(),
                        request.password(),
                        request.nickname()
                );

        return userUseCase.register(command)
                .map(result -> new RegisterUserResponse(
                        result.userId(),
                        result.email(),
                        result.nickname()
                ));
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.OK)
    public Mono<VerifyEmailResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        UserUseCase.VerifyEmailCommand command =
                new UserUseCase.VerifyEmailCommand(request.email(), request.code());

        return userUseCase.verifyEmail(command)
                .map(result -> new VerifyEmailResponse(result.message(), result.verified()));
    }

    public record RegisterUserRequest(
            @NotBlank @Email String email,
            @NotBlank
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,20}$",
                    message = "비밀번호는 6-20자이며, 대소문자와 숫자를 포함해야 합니다.")
            String password,
            @NotBlank
            @Pattern(regexp = "^[가-힣a-zA-Z0-9]{1,10}$",
                    message = "닉네임은 1-10자의 한글, 영문, 숫자만 허용됩니다.")
            String nickname
    ) {}

    public record RegisterUserResponse(
            String userId,
            String email,
            String nickname
    ) {}
    public record VerifyEmailRequest(
            @NotBlank @Email String email,
            @NotBlank
            @Pattern(regexp = "^\\d{6}$", message = "인증코드는 6자리 숫자여야 합니다.")
            String code
    ) {}

    public record VerifyEmailResponse(String message, boolean verified) {}
}