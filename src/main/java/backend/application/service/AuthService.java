package backend.application.service;

import backend.application.port.in.AuthUseCase;
import backend.application.port.out.auth.MagicLinkPort;
import backend.application.port.out.auth.PasswordEncodingPort;
import backend.application.port.out.auth.TokenServicePort;
import backend.application.port.out.auth.VerificationCodePort;
import backend.application.port.out.common.EmailServicePort;
import backend.application.port.out.user.*;
import backend.domain.user.model.*;
import backend.exception.user.UserErrorCode;
import backend.exception.user.UserException;
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
        return userRepository.findByEmail(command.email())
                .switchIfEmpty(Mono.error(new UserException(UserErrorCode.USER_NOT_FOUND)))
                .flatMap(user -> validatePassword(user, command.password()))
                .flatMap(this::generateTokens);
    }

    @Override
    public Mono<RefreshResult> refresh(RefreshCommand command) {
        String refreshToken = command.refreshToken();

        String userId = tokenService.getUserIdFromToken(refreshToken);

        return tokenService.validateRefreshToken(userId, refreshToken)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.error(new UserException(UserErrorCode.INVALID_REFRESH_TOKEN));
                    }
                    return userRepository.findById(UserId.of(Long.valueOf(userId)))
                            .switchIfEmpty(Mono.error(new UserException(UserErrorCode.USER_NOT_FOUND)));
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

        String userId = tokenService.getUserIdFromToken(refreshToken);
        return tokenService.deleteRefreshToken(userId);
    }

    private Mono<User> validatePassword(User user, String rawPassword) {
        String encodedPassword = user.getPassword();

        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            return Mono.error(new UserException(UserErrorCode.INVALID_PASSWORD));
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
                        user.getEmail(),
                        user.getNickname()
                ));
    }

    @Override
    public Mono<Void> sendMagicLink(SendMagicLinkCommand command) {
        String email = command.email();
        return userRepository.existsByEmail(email)
                .filter(exists -> exists)
                .switchIfEmpty(Mono.error(new UserException(UserErrorCode.USER_NOT_FOUND)))
                .then(Mono.defer(() -> {
                    String token = magicLinkPort.generateMagicLinkToken();
                    String magicLink = magicLinkBaseUrl + "/auth/magic-login?token=" + token;
                    return magicLinkPort.storeMagicLinkToken(email, token, magicLinkExpirationMs)
                            .then(emailService.sendMagicLinkEmail(email, magicLink));
                }));
    }

    @Override
    public Mono<LoginResult> verifyMagicLink(VerifyMagicLinkCommand command) {
        String token = command.token();

        return findEmailByMagicLinkToken(token)
                .switchIfEmpty(Mono.error(new UserException(UserErrorCode.INVALID_MAGIC_LINK)))
                .flatMap(email -> {
                    return userRepository.findByEmail(email)
                            .switchIfEmpty(Mono.error(new UserException(UserErrorCode.USER_NOT_FOUND)))
                            .flatMap(user -> magicLinkPort.deleteMagicLinkToken(email)
                                    .then(generateTokens(user)));
                });
    }

    private Mono<String> findEmailByMagicLinkToken(String token) {
        return magicLinkPort.findEmailByToken(token);
    }

    @Override
    public Mono<Void> sendVerificationCode(SendVerificationCodeCommand command) {
        String email = command.email();
        return checkEmailNotExists(email)
                .then(Mono.defer(() -> {
                    String code = verificationCodePort.generateVerificationCode();
                    return verificationCodePort.storeVerificationCode(email, code, codeExpirationMs)
                            .then(emailService.sendVerificationEmail(email, code));
                }));
    }

    private Mono<Void> checkEmailNotExists(String email) {
        return userRepository.existsByEmail(email)
                .flatMap(exists -> exists
                        ? Mono.error(new UserException(UserErrorCode.EMAIL_ALREADY_EXISTS))
                        : Mono.empty());
    }

}
