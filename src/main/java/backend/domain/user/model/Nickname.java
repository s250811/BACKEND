package backend.domain.user.model;

import backend.domain.common.ValueObject;
import lombok.Value;
import java.util.regex.Pattern;

@Value
public class Nickname extends ValueObject {
    private static final Pattern NICKNAME_PATTERN =
            Pattern.compile("^[가-힣a-zA-Z0-9]{1,10}$");

    String value;

    public Nickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임은 필수입니다.");
        }
        if (!NICKNAME_PATTERN.matcher(nickname).matches()) {
            throw new IllegalArgumentException(
                    "닉네임은 1-10자의 한글, 영문, 숫자만 허용됩니다."
            );
        }
        this.value = nickname.trim();
    }
}

