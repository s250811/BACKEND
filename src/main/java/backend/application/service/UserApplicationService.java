package backend.application.service;

import backend.application.port.in.RegisterUserUseCase;
import backend.application.port.out.EmailServicePort;
import backend.application.port.out.TokenServicePort;
import backend.application.port.out.UserRepositoryPort;
import backend.domain.user.model.Email;
import backend.domain.user.model.Nickname;
import backend.domain.user.model.Password;
import backend.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Transactional
public class UserApplicationService implements RegisterUserUseCase {

    private final UserRepositoryPort userRepository;
    private final EmailServicePort emailService;
    private final TokenServicePort tokenService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<RegisterUserResult> register(RegisterUserCommand command) {
        return validateEmailNotExists(command.email())
                .then(createUser(command))
                .flatMap(this::saveUser)
                .flatMap(this::sendVerificationEmail)
                .map(this::toRegisterResult);
    }

    private Mono<Void> validateEmailNotExists(String email) {
        Email emailVO = new Email(email);
        return userRepository.existsByEmail(emailVO)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException("이미 가입된 이메일입니다."));
                    }
                    return Mono.empty();
                });
    }

    private Mono<User> createUser(RegisterUserCommand command) {
        return Mono.fromCallable(() -> {
            Email email = new Email(command.email());
            Password password = new Password(command.password());
            String encodedPassword = password.encode(passwordEncoder);
            Password encodedPasswordVO = new Password(encodedPassword);
            Nickname nickname = new Nickname(command.nickname());

            return User.create(email, encodedPasswordVO, nickname);
        });
    }

    private Mono<User> saveUser(User user) {
        return userRepository.save(user);
    }

    private Mono<User> sendVerificationEmail(User user) {
        String code = tokenService.generateVerificationCode();
        return emailService.sendVerificationEmail(user.getEmail().getValue(), code)
                .thenReturn(user);
    }

    private RegisterUserResult toRegisterResult(User user) {
        return new RegisterUserUseCase.RegisterUserResult(
                user.getId().getValue().toString(),
                user.getEmail().getValue(),
                user.getNickname().getValue()
        );
    }
}
