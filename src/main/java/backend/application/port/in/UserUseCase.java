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
            Long userId,
            String email,
            String nickname
    ) {}
    record UserProfileResult(
            Long userId,
            String email,
            String nickname,
            String profileImageUrl
    ) {}
    record UpdateProfileCommand(
            Long userId,
            String nickname,
            FilePart file
    ) {}
    record UpdateProfileResult(
            Long userId,
            String email,
            String nickname,
            String profileImageUrl
    ) {}
    record CheckEmailDuplicateCommand(String email){}

    Mono<RegisterUserResult> register(RegisterUserCommand command, String verificationCode);
    Mono<UserProfileResult>  getUserProfile(Long id);
    Mono<UpdateProfileResult> updateProfile(UpdateProfileCommand command);
    Mono<Void> checkEmailDuplicate(CheckEmailDuplicateCommand command);
}
