package backend.application.port.out.common;

import reactor.core.publisher.Mono;

public interface EmailServicePort {
    Mono<Void> sendVerificationEmail(String to, String code);
    Mono<Void> sendMagicLinkEmail(String to, String magicLink);
}
