package backend.exception.user;

import backend.exception.ErrorCode;
import backend.exception.ErrorCodeHolder;
import lombok.Getter;

@Getter
public class UserException extends RuntimeException implements ErrorCodeHolder {
    private final ErrorCode errorCode;

    public UserException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
