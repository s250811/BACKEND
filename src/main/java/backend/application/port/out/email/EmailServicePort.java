package backend.application.port.out.email;

import reactor.core.publisher.Mono;

public interface EmailServicePort {
    Mono<Void> sendVerificationEmail(String to, String code);
    Mono<Void> sendMagicLinkEmail(String to, String magicLink);
}
