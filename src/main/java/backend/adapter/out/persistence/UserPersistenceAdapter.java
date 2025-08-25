package backend.adapter.out.persistence;

import backend.application.port.out.UserRepositoryPort;
import backend.domain.user.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserR2dbcRepository repository;
    @Override
    public Mono<User> save(User user) {
        UserEntity entity = toEntity(user);
        return repository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Mono<User> findById(UserId userId) {
        return repository.findById(userId.getValue())
                .map(this::toDomain);
    }

    @Override
    public Mono<User> findByEmail(Email email) {
        return repository.findByEmail(email.getValue())
                .map(this::toDomain);
    }

    @Override
    public Mono<Boolean> existsByEmail(Email email) {
        return repository.existsByEmail(email.getValue());
    }

    private UserEntity toEntity(User user) {
        return UserEntity.builder()
                .id(user.getId().getValue())
                .email(user.getEmail().getValue())
                .password(user.getPassword().getValue())
                .nickname(user.getNickname().getValue())
                .profileImageUrl(user.getProfileImageUrl())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private User toDomain(UserEntity entity) {
        return User.builder()
                .id(UserId.of(entity.getId()))
                .email(new Email(entity.getEmail()))
                .password(new Password(entity.getPassword()))
                .nickname(new Nickname(entity.getNickname()))
                .profileImageUrl(entity.getProfileImageUrl())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}