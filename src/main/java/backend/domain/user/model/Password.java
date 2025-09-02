package backend.domain.user.model;

import backend.domain.common.ValueObject;
import lombok.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.regex.Pattern;

@Value
public class Password extends ValueObject {
    String value;

    public Password(String password) {
        this.value = password;
    }

    public String encode(PasswordEncoder encoder) {
        return encoder.encode(this.value);
    }

    public boolean matches(String encodedPassword) {
        return this.value.equals(encodedPassword);
    }
}

