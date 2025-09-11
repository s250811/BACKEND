package backend.exception.task;

import backend.exception.ErrorCode;

public class TaskException extends RuntimeException {
    private final ErrorCode errorCode;

    public TaskException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
