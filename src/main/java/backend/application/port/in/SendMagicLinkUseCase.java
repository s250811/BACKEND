package backend.application.port.in;

import reactor.core.publisher.Mono;

public interface SendMagicLinkUseCase {

    record SendMagicLinkCommand(String email) {}

    record SendMagicLinkResult(
            String message,
            boolean sent
    ) {}

    Mono<SendMagicLinkResult> sendMagicLink(SendMagicLinkCommand command);

    record VerifyMagicLinkCommand(String token) {}

    record VerifyMagicLinkResult(
            String accessToken,
            String userId,
            String email,
            String nickname
    ) {}

    Mono<VerifyMagicLinkResult> verifyMagicLink(VerifyMagicLinkCommand command);
}
