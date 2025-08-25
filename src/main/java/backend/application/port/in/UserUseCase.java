package backend.application.port.in;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface UserUseCase {
    record RegisterUserCommand(
            String email,
            String password,
            String nickname
    ) {}

    record RegisterUserResult(
            String userId,
            String email,
            String nickname
    ) {}

    record UserProfileResult(
            String userId,
            String email,
            String nickname,
            String profileImageUrl
    ) {}

    record UpdateProfileCommand(
            String userId,
            String nickname,
            FilePart file
    ) {}

    record UpdateProfileResult(
            String userId,
            String email,
            String nickname,
            String profileImageUrl
    ) {}

    Mono<RegisterUserResult> register(RegisterUserCommand command, String verificationCode);
    Mono<UserProfileResult>  getUserProfile(String s);
    Mono<UpdateProfileResult> updateProfile(UpdateProfileCommand command);
}
