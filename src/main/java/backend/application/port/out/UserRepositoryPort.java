package backend.application.port.out;

import backend.domain.user.model.*;
import reactor.core.publisher.Mono;

public interface UserRepositoryPort {
    Mono<User> save(User user);
    Mono<User> findById(UserId userId);
    Mono<User> findByEmail(Email email);
    Mono<Boolean> existsByEmail(Email email);
}
