package backend.application.service;

import backend.application.port.in.LoginUserUseCase;
import backend.application.port.in.SendMagicLinkUseCase;
import backend.application.port.out.*;
import backend.domain.user.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthApplicationService implements LoginUserUseCase, SendMagicLinkUseCase {

    private final UserRepositoryPort userRepository;
    private final MagicLinkTokenRepositoryPort magicLinkTokenRepository;
    private final EmailServicePort emailService;
    private final TokenServicePort tokenService;
    private final PasswordEncoder passwordEncoder;
    @Value("${jwt.magic-link-expiration:600000}")
    private long magicLinkExpirationMs;

    @Override
    public Mono<LoginResult> login(LoginCommand command) {
        Email email = new Email(command.email());

        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("존재하지 않는 이메일 주소입니다.")))
                .flatMap(user -> validatePassword(user, command.password()))
                .map(this::generateLoginResult);
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
                .map(this::generateLoginResult)
                .map(result -> new VerifyMagicLinkResult(
                        result.accessToken(),
                        result.userId(),
                        result.email(),
                        result.nickname()
                ));
    }

    private Mono<User> validatePassword(User user, String rawPassword) {
        if (!user.isPasswordMatch(rawPassword, passwordEncoder)) {
            return Mono.error(new IllegalArgumentException("비밀번호가 올바르지 않습니다."));
        }
        return Mono.just(user);
    }

    private LoginResult generateLoginResult(User user) {
        String accessToken = tokenService.generateAccessToken(
                user.getId().getValue().toString(),
                user.getEmail().getValue()
        );

        return new LoginResult(
                accessToken,
                user.getId().getValue().toString(),
                user.getEmail().getValue(),
                user.getNickname().getValue()
        );
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
