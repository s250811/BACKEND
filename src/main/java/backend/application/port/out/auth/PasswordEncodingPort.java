package backend.application.port.out.auth;

public interface PasswordEncodingPort {
    String encode(String rawPassword);
    boolean matches(String rawPassword, String encodedPassword);
}