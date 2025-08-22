package backend.application.service;

import backend.application.port.in.UserUseCase;
import backend.application.port.out.EmailServicePort;
import backend.application.port.out.FileStoragePort;
import backend.application.port.out.TokenServicePort;
import backend.application.port.out.UserRepositoryPort;
import backend.domain.user.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService implements UserUseCase {
    @Value("${verification.code-expiration:60000}")
    private long codeExpirationMs;

    private final FileStoragePort fileStoragePort;
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
            String nicknameValue = (command.nickname() == null || command.nickname().trim().isEmpty())
                    ? tokenService.generateRandomNickname() : command.nickname();
            Nickname nickname = new Nickname(nicknameValue);
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

    @Cacheable(value = "user:profile", key = "#userId")
    @Transactional(readOnly = true)
    public Mono<UserProfileResult> getUserProfile(String userId) {
        return userRepository.findById(UserId.of(UUID.fromString(userId)))
                .map(user -> new UserProfileResult(
                        user.getId().getValue().toString(),
                        user.getEmail().getValue(),
                        user.getNickname().getValue(),
                        user.getProfileImageUrl()
                ));
    }
    @Override
    @CacheEvict(value = "user:profile", key = "#command.userId")
    @Transactional
    public Mono<UpdateProfileResult> updateProfile(UpdateProfileCommand command) {
        return userRepository.findById(UserId.of(UUID.fromString(command.userId())))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("사용자를 찾을 수 없습니다.")))
                .flatMap(user -> {
                    String oldImageUrl = user.getProfileImageUrl();

                    Mono<String> imageUrlMono = command.file() != null
                            ? fileStoragePort.uploadFile(command.file(), "profiles")
                            : Mono.just(user.getProfileImageUrl());

                    return imageUrlMono.flatMap(imageUrl -> {
                        String nicknameValue = command.nickname() != null
                                ? command.nickname()
                                : user.getNickname().getValue();

                        Nickname newNickname = new Nickname(nicknameValue);
                        user.updateProfile(newNickname, imageUrl);
                        return userRepository.save(user)
                                .doOnSuccess(savedUser -> {
                                    // 트랜잭션 커밋 후 기존 이미지는 비동기로 별도 스레드에서 삭제 (실패해도 메인 플로우에 영향 X)
                                    if (command.file() != null && oldImageUrl != null && !oldImageUrl.equals(imageUrl)) {
                                        deleteImageAsync(oldImageUrl);
                                    }
                                });
                    });
                })
                .map(user -> new UpdateProfileResult(
                        user.getId().getValue().toString(),
                        user.getEmail().getValue(),
                        user.getNickname().getValue(),
                        user.getProfileImageUrl()
                ));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)

    void deleteImageAsync(String imageUrl) {
        Mono.fromRunnable(() ->
                        fileStoragePort.deleteFile(imageUrl)
                                .doOnError(error -> log.warn("기존 이미지 삭제 실패: {}", imageUrl, error))
                                .doOnSuccess(v -> log.debug("기존 이미지 삭제 완료: {}", imageUrl))
                                .subscribe())
                // 별도 스레드에서 수행
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

}
