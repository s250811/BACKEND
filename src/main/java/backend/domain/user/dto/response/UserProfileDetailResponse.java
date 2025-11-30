package backend.domain.user.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserProfileDetailResponse(
        Long userId,
        String email,
        @NotBlank
        @Pattern(regexp = "^[가-힣a-zA-Z0-9]{1,10}$",
                message = "닉네임은 1-10자의 한글, 영문, 숫자만 허용됩니다.")
        String nickname,
        String profileImageUrl
) {}
