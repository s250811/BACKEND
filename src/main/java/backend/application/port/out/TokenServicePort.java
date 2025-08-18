package backend.application.port.out;

public interface TokenServicePort {
    String generateAccessToken(String userId, String email);
    String generateMagicLinkToken();
    String generateVerificationCode();
}
