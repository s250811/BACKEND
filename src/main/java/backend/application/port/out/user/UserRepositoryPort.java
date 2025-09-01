package backend.application.port.out.user;

import backend.domain.user.model.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepositoryPort {
    Mono<User> save(User user);
    Mono<User> findById(UserId userId);
    Mono<User> findByEmail(Email email);
    Mono<Boolean> existsByEmail(Email emailVO);
    Flux<User> findAllById(Iterable<Long> userIds);
}