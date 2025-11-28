package backend.infrastructure.adapter.out.persistence.db.r2dbc.user;

import backend.application.port.out.user.UserRepositoryPort;
import backend.domain.user.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
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
        return repository.findById(userId.value())
                .map(this::toDomain);
    }

    @Override
    public Mono<User> findByEmail(String email) {
        return repository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public Flux<User> findAllById(Iterable<Long> userIds) {
        return repository.findAllById(userIds)
                .map(this::toDomain);
    }

    private UserEntity toEntity(User user) {
        return UserEntity.builder()
                .id(user.getIdValue())
                .email(user.getEmail())
                .password(user.getPassword())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private User toDomain(UserEntity entity) {
        return User.builder()
                .id(UserId.of(entity.getId()))
                .email(entity.getEmail())
                .password(entity.getPassword())
                .nickname(entity.getNickname())
                .profileImageUrl(entity.getProfileImageUrl())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}