package backend.adapter.out.email;

import backend.application.port.out.EmailServicePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 이메일 전송 기능 out port 구현체 (port → repository 호출)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailAdapter implements EmailServicePort {

    private final JavaMailSender mailSender;

    @Override
    public Mono<Void> sendVerificationEmail(String to, String code) {
        return Mono.fromRunnable(() -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setText("인증 코드: " + code);

            mailSender.send(message);
            log.info("Verification email sent to: {}", to);
        });
    }

    @Override
    public Mono<Void> sendMagicLinkEmail(String to, String magicLink) {
        return Mono.fromRunnable(() -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setText("로그인 링크: " + magicLink + "\n\n이 링크는 10분간 유효합니다.");

            mailSender.send(message);
            log.info("Magic link email sent to: {}", to);
        });
    }
}