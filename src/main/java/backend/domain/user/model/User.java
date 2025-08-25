package backend.domain.user.model;

import backend.domain.common.AggregateRoot;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;

@Getter
public class User extends AggregateRoot<UserId> {
    private Email email;
    private Password password;
    private Nickname nickname;
    private String profileImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public User(UserId id, Email email, Password password, Nickname nickname,
                String profileImageUrl, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User create(Email email, Password password, Nickname nickname) {
        return User.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .build();
    }

    public Long getIdValue() {
        return this.id != null ? this.id.getValue() : null;
    }

    public boolean isPasswordMatch(String rawPassword, PasswordEncoder encoder) {
        return this.password.matches(rawPassword, encoder);
    }

    public void updateProfile(Nickname nickname, String profileImageUrl) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void changePassword(Password newPassword) {
        this.password = newPassword;
        this.updatedAt = LocalDateTime.now();
    }
}

