package backend.exception.messaging;

import backend.exception.ErrorCode;
import backend.exception.ErrorCodeHolder;
import lombok.Getter;

@Getter
public class MessagingException extends RuntimeException implements ErrorCodeHolder {
    private final ErrorCode errorCode;

    public MessagingException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
