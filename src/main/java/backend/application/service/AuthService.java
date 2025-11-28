package backend.application.service;

import backend.application.port.in.auth.AuthUseCase;
import backend.application.port.out.auth.MagicLinkPort;
import backend.application.port.out.auth.PasswordEncodingPort;
import backend.application.port.out.auth.TokenServicePort;
import backend.application.port.out.auth.VerificationCodePort;
import backend.application.port.out.email.EmailServicePort;
import backend.application.port.out.user.*;
import backend.domain.user.dto.requst.LoginRequest;
import backend.domain.user.dto.response.LoginResponse;
import backend.domain.user.dto.response.RefreshResponse;
import backend.domain.user.model.*;
import backend.exception.user.UserErrorCode;
import backend.exception.user.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

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
    public Mono<Tuple3<LoginResponse,String, Long>>  login(LoginRequest request) {
        return userRepository.findByEmail(request.email())
                .switchIfEmpty(Mono.error(new UserException(UserErrorCode.USER_NOT_FOUND)))
                .flatMap(user -> validatePassword(user, request.password()))
                .flatMap(this::generateTokens);
    }

    @Override
    public Mono<RefreshResponse> refresh(String refreshToken) {
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
                    String newAccessToken = tokenService.generateAccessToken(user.getIdValue().toString());
                    String newRefreshToken = tokenService.generateRefreshToken(user.getIdValue().toString());

                    return tokenService.storeRefreshToken(user.getId().value().toString(), newRefreshToken)
                            .thenReturn(new RefreshResponse(newAccessToken, newRefreshToken, refreshTokenExpiration / 1000));
                });
    }

    @Override
    public Mono<Void> logout(String refreshToken) {
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

    private Mono<Tuple3<LoginResponse,String, Long>> generateTokens(User user) {
        String accessToken = tokenService.generateAccessToken(user.getId().value().toString());
        String refreshToken = tokenService.generateRefreshToken(user.getId().value().toString());

        return tokenService.storeRefreshToken(user.getId().value().toString(), refreshToken)
                .thenReturn(new LoginResponse(
                        accessToken,
                        user.getId().value().toString(),
                        user.getEmail(),
                        user.getNickname()
                ))
                .map(response -> Tuples.of(response, refreshToken, refreshTokenExpiration / 1000));
    }

    @Override
    public Mono<Void> sendMagicLink(String email) {
        return userRepository.existsByEmail(email)
                .filter(exists -> exists)
                .switchIfEmpty(Mono.error(new UserException(UserErrorCode.USER_NOT_FOUND)))
                .then(Mono.defer(() -> { // 구독 시점에 실행 (지연 계산)
                    String token = magicLinkPort.generateMagicLinkToken();
                    String magicLink = magicLinkBaseUrl + "/auth/magic-login?token=" + token;
                    return magicLinkPort.storeMagicLinkToken(email, token, magicLinkExpirationMs)
                            .then(emailService.sendMagicLinkEmail(email, magicLink));
                }));
    }

    @Override
    public Mono<Tuple3<LoginResponse,String, Long>> verifyMagicLink(String token) {
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
    public Mono<Void> sendVerificationCode(String email) {
        return Mono.defer(() -> { // 구독 시점에 실행 (지연 계산)
                    String code = verificationCodePort.generateVerificationCode();
                    return verificationCodePort.storeVerificationCode(email, code, codeExpirationMs)
                            .then(emailService.sendVerificationEmail(email, code));
                });
    }
}
