package backend.domain.user.dto.response;

public record RefreshResponse(
        String accessToken,
        String refreshToken,
        long refreshTokenExpiration
) {}

