package backend.domain.user.dto.response;

public record LoginResponse(
        String accessToken,
        String userId,
        String email,
        String nickname
) {}
