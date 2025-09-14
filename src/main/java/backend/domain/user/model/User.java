package backend.domain.user.model;

import backend.domain.common.AggregateRoot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class User extends AggregateRoot<UserId> {
    private UserId id;
    private String email;
    private String password;
    private String nickname;
    private String profileImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static User create(String email, String password, String nickname) {
        return User.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .build();
    }

    public Long getIdValue() {
        return this.id != null ? this.id.getValue() : null;
    }

    public void updateProfile(String nickname, String profileImageUrl) {
        this.nickname = nickname != null ? nickname : this.nickname;
        this.profileImageUrl = profileImageUrl != null ? profileImageUrl : this.profileImageUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public static String generateRandomNickname() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}

