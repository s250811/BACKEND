package backend.application.service;

import backend.application.port.in.user.UserUseCase;
import backend.application.port.out.file.FileStoragePort;
import backend.application.port.out.auth.PasswordEncodingPort;
import backend.application.port.out.user.UserRepositoryPort;
import backend.application.port.out.auth.VerificationCodePort;
import backend.domain.user.dto.requst.RegisterUserRequest;
import backend.domain.user.dto.response.RegisterUserResponse;
import backend.domain.user.dto.response.UserProfileDetailResponse;
import backend.domain.user.model.*;
import backend.exception.user.UserErrorCode;
import backend.exception.user.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService implements UserUseCase {

    private final FileStoragePort fileStoragePort;
    private final UserRepositoryPort userRepository;
    private final VerificationCodePort verificationCodeService;
    private final PasswordEncodingPort passwordEncoder;

    @Override
    public Mono<Void> checkEmailDuplicate(String email) {
        return userRepository.existsByEmail(email)
                .flatMap(exists -> exists
                        ? Mono.error(new UserException(UserErrorCode.EMAIL_ALREADY_EXISTS))
                        : Mono.empty());
    }

    @Override
    public Mono<RegisterUserResponse> register(RegisterUserRequest request) {
        return verificationCodeService.getVerificationCode(request.email())
                .switchIfEmpty(Mono.error(new UserException(UserErrorCode.INVALID_VERIFICATION_CODE)))
                .flatMap(storedCode -> {
                    if (!storedCode.equals(request.code())) {
                        return Mono.error(new UserException(UserErrorCode.INVALID_VERIFICATION_CODE));
                    }
                    return createUser(request)
                            .flatMap(this::saveUser)
                            .flatMap(user -> verificationCodeService.deleteVerificationCode(request.email())
                                    .thenReturn(user))
                            .map(this::toRegisterResult);
                });
    }

    private Mono<User> createUser(RegisterUserRequest request) {
        return Mono.fromCallable(() -> {
            String email = request.email();
            String password = request.password();
            String encodedPassword = passwordEncoder.encode(password);
            String nickname = (request.nickname() == null || request.nickname().trim().isEmpty())
                    ? User.generateRandomNickname() : request.nickname();
            return User.create(email, encodedPassword, nickname);
        });
    }

    private Mono<User> saveUser(User user) {
        return userRepository.save(user);
    }

    private RegisterUserResponse toRegisterResult(User user) {
        return new RegisterUserResponse(
                user.getId().value(),
                user.getEmail(),
                user.getNickname()
        );
    }
    @Override
    @Cacheable(value = "user:profile", key = "#userId")
    @Transactional(readOnly = true)
    public Mono<UserProfileDetailResponse> getUserProfile(Long userId) {
        return userRepository.findById(UserId.of(userId))
                .switchIfEmpty(Mono.error(new UserException(UserErrorCode.USER_NOT_FOUND)))
                .map(user -> new UserProfileDetailResponse(
                        user.getId().value(),
                        user.getEmail(),
                        user.getNickname(),
                        user.getProfileImageUrl()
                ));
    }
    @Override
    @CacheEvict(value = "user:profile", key = "#command.userId")
    @Transactional
    public Mono<UserProfileDetailResponse> updateProfile(Long userId, String nickname, FilePart file) {
        return userRepository.findById(UserId.of(Long.valueOf(userId)))
                .switchIfEmpty(Mono.error(new UserException(UserErrorCode.USER_NOT_FOUND)))
                .flatMap(user -> {
                    String oldImageUrl = user.getProfileImageUrl();

                    Mono<String> imageUrlMono = file != null
                            ? fileStoragePort.uploadFile(file, "profiles")
                            : Mono.just(user.getProfileImageUrl());

                    return imageUrlMono.flatMap(imageUrl -> {
                        user.updateProfile(nickname, imageUrl);
                        return userRepository.save(user)
                                .doOnSuccess(savedUser -> {
                                    // 트랜잭션 커밋 후 기존 이미지는 비동기로 별도 스레드에서 삭제 (실패해도 메인 플로우에 영향 X)
                                    if (file != null && oldImageUrl != null && !oldImageUrl.equals(imageUrl)) {
                                        deleteImageAsync(oldImageUrl);
                                    }
                                });
                    });
                })
                .map(user -> new UserProfileDetailResponse(
                        user.getId().value(),
                        user.getEmail(),
                        user.getNickname(),
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
