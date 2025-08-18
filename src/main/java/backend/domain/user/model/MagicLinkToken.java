package backend.domain.user.model;

import backend.domain.common.AggregateRoot;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class MagicLinkToken extends AggregateRoot<UUID> {
    private Email email;
    private String token;
    private LocalDateTime expiresAt;
    private boolean used;

    @Builder
    public MagicLinkToken(UUID id, Email email, String token,
                          LocalDateTime expiresAt, boolean used) {
        this.id = id != null ? id : UUID.randomUUID();
        this.email = email;
        this.token = token;
        this.expiresAt = expiresAt;
        this.used = used;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static MagicLinkToken create(Email email, String token, long expirationMinutes){
        return MagicLinkToken.builder()
                .email(email)
                .token(token)
                .expiresAt(LocalDateTime.now().plusMinutes(expirationMinutes))
                .used(false)
                .build();
    }

    public boolean isValid() {
        return !used && LocalDateTime.now().isBefore(expiresAt);
    }

    public void markAsUsed() {
        this.used = true;
        this.updatedAt = LocalDateTime.now();
    }
}

