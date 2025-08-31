package backend.adapter.in.web;

import backend.application.port.in.UserUseCase;
import backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
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
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserUseCase userUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        UserUseCase.RegisterUserCommand command =
                new UserUseCase.RegisterUserCommand(
                        request.email(),
                        request.password(),
                        request.nickname()
                );

        return userUseCase.register(command, request.code())
                .map(result -> new RegisterUserResponse(
                        result.userId(),
                        result.email(),
                        result.nickname()
                ));
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<UserProfileResponse>> getProfile() {
        return SecurityUtils.getCurrentUserId()
                .flatMap(userUseCase::getUserProfile)
                .map(result -> ResponseEntity.ok(new UserProfileResponse(
                        result.userId(),
                        result.email(),
                        result.nickname(),
                        result.profileImageUrl()
                )))
                .onErrorReturn(ResponseEntity.badRequest().build());
    }

    @PatchMapping("/me")
    public Mono<ResponseEntity<UpdateProfileResponse>> updateProfile(
            @Pattern(regexp = "^[가-힣a-zA-Z0-9]{1,10}$",
                    message = "닉네임은 1-10자의 한글, 영문, 숫자만 허용됩니다.")
            @RequestPart(value = "nickname", required = false) String nickname,
            @RequestPart(value = "file", required = false) FilePart file) {

        return SecurityUtils.getCurrentUserId()
                .flatMap(userId -> {
                    var command = new UserUseCase.UpdateProfileCommand(
                            userId,
                            nickname,
                            file
                    );
                    return userUseCase.updateProfile(command);
                })
                .map(result -> ResponseEntity.ok(new UpdateProfileResponse(
                        result.userId(),
                        result.email(),
                        result.nickname(),
                        result.profileImageUrl()
                )))
                .onErrorReturn(ResponseEntity.badRequest().build());
    }


    public record RegisterUserRequest(
            @NotBlank @Email String email,
            @NotBlank
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,20}$",
                    message = "비밀번호는 6-20자이며, 대소문자와 숫자를 포함해야 합니다.")
            String password,
            @Pattern(regexp = "^[가-힣a-zA-Z0-9]{1,10}$",
                    message = "닉네임은 비어있거나 1-10자의 한글, 영문, 숫자여야 합니다.")
            String nickname,
            @NotBlank
            @Pattern(regexp = "^\\d{6}$", message = "인증코드는 6자리 숫자여야 합니다.")
            String code
    ) {}

    public record RegisterUserResponse(
            String userId,
            String email,
            String nickname
    ) {}

    public record UserProfileResponse(
            String userId,
            String email,
            @NotBlank
            @Pattern(regexp = "^[가-힣a-zA-Z0-9]{1,10}$",
                    message = "닉네임은 1-10자의 한글, 영문, 숫자만 허용됩니다.")
            String nickname,
            String profileImageUrl
    ) {}

    public record UpdateProfileResponse(
            String userId,
            String email,
            String nickname,
            String profileImageUrl
    ) {}
}