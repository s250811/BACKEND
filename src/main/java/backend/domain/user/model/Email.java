package backend.domain.user.model;

import backend.domain.common.ValueObject;
import lombok.Value;
import java.util.regex.Pattern;

@Value
public class Email extends ValueObject {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    String value;

    public Email(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }
        this.value = email.toLowerCase().trim();
    }
}

