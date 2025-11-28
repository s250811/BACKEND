package backend.application.port.in.user;

import backend.domain.user.dto.requst.RegisterUserRequest;
import backend.domain.user.dto.response.RegisterUserResponse;
import backend.domain.user.dto.response.UserProfileDetailResponse;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface UserUseCase {
    Mono<RegisterUserResponse> register(RegisterUserRequest request);
    Mono<UserProfileDetailResponse>  getUserProfile(Long id);
    Mono<UserProfileDetailResponse> updateProfile(Long userId, String nickname, FilePart file);
    Mono<Void> checkEmailDuplicate(String email);
}
