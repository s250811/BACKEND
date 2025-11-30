package backend.exception.task;

import backend.exception.ErrorCode;
import backend.exception.ErrorCodeHolder;
import lombok.Getter;

@Getter
public class TaskException extends RuntimeException implements ErrorCodeHolder {
    private final ErrorCode errorCode;

    public TaskException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
