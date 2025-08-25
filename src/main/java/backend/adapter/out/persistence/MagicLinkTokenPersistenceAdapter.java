package backend.adapter.out.persistence;

import backend.application.port.out.MagicLinkTokenRepositoryPort;
import backend.domain.user.model.Email;
import backend.domain.user.model.MagicLinkToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class MagicLinkTokenPersistenceAdapter implements MagicLinkTokenRepositoryPort {

    private final MagicLinkTokenR2dbcRepository repository;

    @Override
    public Mono<MagicLinkToken> save(MagicLinkToken token) {
        MagicLinkTokenEntity entity = toEntity(token);
        return repository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Mono<MagicLinkToken> findByToken(String token) {
        return repository.findByToken(token)
                .map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteByEmail(Email email) {
        return repository.deleteByEmail(email.getValue());
    }

    private MagicLinkTokenEntity toEntity(MagicLinkToken token) {
        return MagicLinkTokenEntity.builder()
                .id(token.getId())
                .email(token.getEmail().getValue())
                .token(token.getToken())
                .expiresAt(token.getExpiresAt())
                .used(token.isUsed())
                .createdAt(token.getCreatedAt())
                .updatedAt(token.getUpdatedAt())
                .build();
    }

    private MagicLinkToken toDomain(MagicLinkTokenEntity entity) {
        return MagicLinkToken.builder()
                .id(entity.getId())
                .email(new Email(entity.getEmail()))
                .token(entity.getToken())
                .expiresAt(entity.getExpiresAt())
                .used(entity.isUsed())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
