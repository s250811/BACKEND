package backend.application.service;

import backend.application.port.in.UserUseCase;
import backend.application.port.out.EmailServicePort;
import backend.application.port.out.TokenServicePort;
import backend.application.port.out.UserRepositoryPort;
import backend.domain.user.model.Email;
import backend.domain.user.model.Nickname;
import backend.domain.user.model.Password;
import backend.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService implements UserUseCase {
    @Value("${verification.code-expiration:60000}")
    private long codeExpirationMs;

    private final UserRepositoryPort userRepository;
    private final EmailServicePort emailService;
    private final TokenServicePort tokenService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<RegisterUserResult> register(RegisterUserCommand command) {
        return validateEmailNotExists(command.email())
                .then(createUser(command))
                .flatMap(this::saveUser)
                .flatMap(this::saveAndSendVerificationCode)
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

    private Mono<User> saveAndSendVerificationCode(User user) {
        String code = tokenService.generateVerificationCode();
        String email = user.getEmail().getValue();

        return tokenService.storeVerificationCode(email, code, codeExpirationMs)
                .then(emailService.sendVerificationEmail(email, code))
                .thenReturn(user);
    }

    private RegisterUserResult toRegisterResult(User user) {
        return new UserUseCase.RegisterUserResult(
                user.getId().getValue().toString(),
                user.getEmail().getValue(),
                user.getNickname().getValue()
        );
    }
    @Override
    public Mono<VerifyEmailResult> verifyEmail(VerifyEmailCommand command) {
        return tokenService.getVerificationCode(command.email())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("인증코드가 만료되었거나 존재하지 않습니다.")))
                .flatMap(storedCode -> {
                    if (!storedCode.equals(command.code())) {
                        return Mono.error(new IllegalArgumentException("잘못된 인증코드입니다."));
                    }
                    return tokenService.deleteVerificationCode(command.email())
                            .thenReturn(new VerifyEmailResult("이메일 인증이 완료되었습니다.", true));
                })
                .onErrorReturn(new VerifyEmailResult("인증에 실패했습니다.", false));
    }
}
