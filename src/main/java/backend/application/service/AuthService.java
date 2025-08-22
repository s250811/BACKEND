package backend.application.service;

import backend.application.port.in.AuthUseCase;
import backend.application.port.out.*;
import backend.domain.user.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final UserRepositoryPort userRepository;
    private final MagicLinkTokenRepositoryPort magicLinkTokenRepository;
    private final EmailServicePort emailService;
    private final TokenServicePort tokenService;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${jwt.magic-link-expiration:600000}")
    private long magicLinkExpirationMs;

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
                    return userRepository.findById(UserId.of(java.util.UUID.fromString(userId)));
                })
                .flatMap(user -> {
                    String newAccessToken = tokenService.generateAccessToken(
                            user.getId().getValue().toString(),
                            user.getEmail().getValue()
                    );
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

    @Override
    public Mono<SendMagicLinkResult> sendMagicLink(SendMagicLinkCommand command) {
        Email email = new Email(command.email());

        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("존재하지 않는 이메일 주소입니다.")))
                .flatMap(user -> createAndSendMagicLink(email))
                .map(unused -> new SendMagicLinkResult("로그인 링크가 이메일로 전송되었습니다.", true))
                .onErrorReturn(new SendMagicLinkResult("메일 전송에 실패했습니다.", false));
    }

    @Override
    public Mono<VerifyMagicLinkResult> verifyMagicLink(VerifyMagicLinkCommand command) {
        return magicLinkTokenRepository.findByToken(command.token())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("유효하지 않은 링크입니다.")))
                .flatMap(this::validateMagicLinkToken)
                .flatMap(token -> userRepository.findByEmail(token.getEmail()))
                .flatMap(user -> {
                    String accessToken = tokenService.generateAccessToken(
                            user.getId().getValue().toString(),
                            user.getEmail().getValue()
                    );
                    String refreshToken = tokenService.generateRefreshToken(user.getId().getValue().toString());

                    return tokenService.storeRefreshToken(user.getId().getValue().toString(), refreshToken)
                            .thenReturn(new VerifyMagicLinkResult(
                                    accessToken,
                                    refreshToken,
                                    refreshTokenExpiration,
                                    user.getId().getValue().toString(),
                                    user.getEmail().getValue(),
                                    user.getNickname().getValue()
                            ));
                });
    }

    private Mono<User> validatePassword(User user, String rawPassword) {
        if (!user.isPasswordMatch(rawPassword, passwordEncoder)) {
            return Mono.error(new IllegalArgumentException("비밀번호가 올바르지 않습니다."));
        }
        return Mono.just(user);
    }

    private Mono<LoginResult> generateTokens(User user) {
        String accessToken = tokenService.generateAccessToken(
                user.getId().getValue().toString(),
                user.getEmail().getValue()
        );
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

    private Mono<Void> createAndSendMagicLink(Email email) {
        return magicLinkTokenRepository.deleteByEmail(email)
                .then(Mono.fromCallable(() -> {
                    String token = tokenService.generateMagicLinkToken();
                    return MagicLinkToken.create(email, token, magicLinkExpirationMs / 60000L);
                }))
                .flatMap(magicLinkTokenRepository::save)
                .flatMap(token -> {
                    String magicLink = "http://localhost:3000/auth/magic-login?token=" + token.getToken();
                    return emailService.sendMagicLinkEmail(email.getValue(), magicLink);
                });
    }

    private Mono<MagicLinkToken> validateMagicLinkToken(MagicLinkToken token) {
        if (!token.isValid()) {
            return Mono.error(new IllegalArgumentException("만료되거나 이미 사용된 링크입니다."));
        }
        token.markAsUsed();
        return magicLinkTokenRepository.save(token);
    }
}
