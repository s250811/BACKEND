package backend.application.service;

import backend.application.port.in.AuthUseCase;
import backend.application.port.out.*;
import backend.domain.user.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final UserRepositoryPort userRepository;
    private final EmailServicePort emailService;
    private final TokenServicePort tokenService;
    private final PasswordEncodingPort passwordEncoder;
    private final VerificationCodePort verificationCodePort;
    private final MagicLinkPort magicLinkPort;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${magic-link.expiration:600000}")
    private long magicLinkExpirationMs;

    @Value("${verification.code-expiration:60000}")
    private long codeExpirationMs;

    @Value("${magic-link.base-url:http://localhost:3000}")
    private String magicLinkBaseUrl;

    @Override
    public Mono<LoginResult> login(LoginCommand command) {
        Email email = new Email(command.email());

        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("존재하지 않는 이메일 주소입니다.")))
                .flatMap(user -> validatePassword(user, command.password()))
                .flatMap(this::generateTokens);
    }

    @Override
    public Mono<RefreshResult> refresh(RefreshCommand command) {
        String refreshToken = command.refreshToken();

        if (!tokenService.validateToken(refreshToken)) {
            return Mono.error(new IllegalArgumentException("Invalid refresh token"));
        }

        String userId = tokenService.getUserIdFromToken(refreshToken);

        return tokenService.validateRefreshToken(userId, refreshToken)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.error(new IllegalArgumentException("Invalid refresh token"));
                    }
                    return userRepository.findById(UserId.of(Long.valueOf(userId)));
                })
                .flatMap(user -> {
                    String newAccessToken = tokenService.generateAccessToken(user.getId().getValue().toString());
                    String newRefreshToken = tokenService.generateRefreshToken(user.getId().getValue().toString());

                    return tokenService.storeRefreshToken(user.getId().getValue().toString(), newRefreshToken)
                            .thenReturn(new RefreshResult(newAccessToken, newRefreshToken, refreshTokenExpiration));
                });
    }

    @Override
    public Mono<Void> logout(LogoutCommand command) {
        String refreshToken = command.refreshToken();

        if (!tokenService.validateToken(refreshToken)) {
            return Mono.empty();
        }

        String userId = tokenService.getUserIdFromToken(refreshToken);
        return tokenService.deleteRefreshToken(userId);
    }

    private Mono<User> validatePassword(User user, String rawPassword) {
        String encodedPassword = user.getPassword().getValue();

        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            return Mono.error(new IllegalArgumentException("비밀번호가 올바르지 않습니다."));
        }
        return Mono.just(user);
    }

    private Mono<LoginResult> generateTokens(User user) {
        String accessToken = tokenService.generateAccessToken(user.getId().getValue().toString());
        String refreshToken = tokenService.generateRefreshToken(user.getId().getValue().toString());

        return tokenService.storeRefreshToken(user.getId().getValue().toString(), refreshToken)
                .thenReturn(new LoginResult(
                        accessToken,
                        refreshToken,
                        refreshTokenExpiration,
                        user.getId().getValue().toString(),
                        user.getEmail().getValue(),
                        user.getNickname().getValue()
                ));
    }

    @Override
    public Mono<Void> sendMagicLink(SendMagicLinkCommand command) {
        Email email = new Email(command.email());

        return userRepository.existsByEmail(email)
                .filter(exists -> exists)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("존재하지 않는 사용자입니다.")))
                .then(Mono.defer(() -> {
                    String token = magicLinkPort.generateMagicLinkToken();
                    String magicLink = magicLinkBaseUrl + "/auth/magic-login?token=" + token;
                    return magicLinkPort.storeMagicLinkToken(email.getValue(), token, magicLinkExpirationMs)
                            .then(emailService.sendMagicLinkEmail(email.getValue(), magicLink));
                }));
    }

    @Override
    public Mono<LoginResult> verifyMagicLink(VerifyMagicLinkCommand command) {
        String token = command.token();

        return findEmailByToken(token)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("유효하지 않거나 만료된 매직링크입니다.")))
                .flatMap(email -> {
                    Email emailVO = new Email(email);
                    return userRepository.findByEmail(emailVO)
                            .switchIfEmpty(Mono.error(new IllegalArgumentException("존재하지 않는 사용자입니다.")))
                            .flatMap(user -> magicLinkPort.deleteMagicLinkToken(email)
                                    .then(generateTokens(user)));
                });
    }

    private Mono<String> findEmailByToken(String token) {
        return magicLinkPort.findEmailByToken(token);
    }

    @Override
    public Mono<Void> sendVerificationCode(SendVerificationCodeCommand command) {
        Email email = new Email(command.email());

        return checkEmailNotExists(email)
                .then(Mono.defer(() -> {
                    String code = verificationCodePort.generateVerificationCode();
                    return verificationCodePort.storeVerificationCode(email.getValue(), code, codeExpirationMs)
                            .then(emailService.sendVerificationEmail(email.getValue(), code));
                }));
    }

    private Mono<Void> checkEmailNotExists(Email email) {
        return userRepository.existsByEmail(email)
                .flatMap(exists -> exists
                        ? Mono.error(new IllegalArgumentException("이미 가입된 이메일입니다."))
                        : Mono.empty());
    }

}
