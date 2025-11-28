package backend.domain.user.dto.requst;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

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
