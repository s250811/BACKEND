package backend.infrastructure.adapter.in.web.rest.user;

import backend.application.port.in.user.UserUseCase;
import backend.domain.user.dto.requst.RegisterUserRequest;
import backend.domain.user.dto.response.RegisterUserResponse;
import backend.domain.user.dto.response.UserProfileDetailResponse;
import backend.infrastructure.adapter.in.web.rest.dto.ApiResponseDto;
import backend.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

/**
 * User 외부 요청을 받는 진입 지점
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserUseCase userUseCase;

    @GetMapping("/emails/availability")
    @Operation(summary = "이메일 중복 검사")
    public Mono<ApiResponseDto<Void>> checkEmailDuplicate(@RequestParam @Email String email) {
        return userUseCase.checkEmailDuplicate(email)
                .then(Mono.just(ApiResponseDto.createSuccessNoContent("이메일 사용 가능합니다.")));
    }

    @Operation(summary = "회원가입")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponseDto<RegisterUserResponse>> register(@Valid @RequestBody RegisterUserRequest request) {
        return userUseCase.register(request)
                .map(result -> new RegisterUserResponse(
                        result.userId(),
                        result.email(),
                        result.nickname()
                ))
                .map(response -> ApiResponseDto.createSuccess(response, "회원가입이 완료되었습니다."));
    }

    @Operation(summary = "프로필 조회")
    @GetMapping("/me")
    public Mono<ApiResponseDto<UserProfileDetailResponse>> getProfile() {
        return SecurityUtils.getCurrentUserId()
                .flatMap(userUseCase::getUserProfile)
                .map(result -> ApiResponseDto.createSuccess(result, "프로필 조회 완료"))
                .onErrorReturn(ApiResponseDto.createError("프로필 조회 실패"));
    }

    @Operation(summary = "프로필 수정")
    @PatchMapping("/me")
    public Mono<ApiResponseDto<UserProfileDetailResponse>> updateProfile(
            @Pattern(regexp = "^[가-힣a-zA-Z0-9]{1,10}$",
                    message = "닉네임은 1-10자의 한글, 영문, 숫자만 허용됩니다.")
            @RequestPart(value = "nickname", required = false) String nickname,
            @RequestPart(value = "file", required = false) FilePart file) {

        return SecurityUtils.getCurrentUserId()
                .flatMap(userId -> userUseCase.updateProfile(userId, nickname, file))
                .map(result -> ApiResponseDto.createSuccess(result, "프로필 수정 완료"))
                .onErrorReturn(ApiResponseDto.createError("프로필 수정 실패"));
    }
}