package backend.exception.user;

import backend.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    INVALID_MAGIC_LINK(HttpStatus.UNAUTHORIZED, "유효하지 않은 매직링크입니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.UNPROCESSABLE_ENTITY, "유효하지 않은 검증 코드입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
