package backend.domain.user.model;

import backend.domain.common.ValueObject;
import lombok.Value;

import java.util.UUID;
import java.util.regex.Pattern;

@Value
public class Nickname extends ValueObject {
    private static final Pattern NICKNAME_PATTERN =
            Pattern.compile("^[가-힣a-zA-Z0-9]{1,10}$");

    String value;

    public Nickname(String nickname) {
        this.value = nickname.trim();
    }

    public static String generateRandomNickname() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}

