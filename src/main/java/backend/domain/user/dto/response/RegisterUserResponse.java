package backend.domain.user.dto.response;

public record RegisterUserResponse(
        Long userId,
        String email,
        String nickname
) {}
