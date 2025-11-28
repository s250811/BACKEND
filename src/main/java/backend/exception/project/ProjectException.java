package backend.exception.project;

import backend.exception.ErrorCode;
import backend.exception.ErrorCodeHolder;
import lombok.Getter;

@Getter
public class ProjectException extends RuntimeException implements ErrorCodeHolder {
    private final ErrorCode errorCode;

    public ProjectException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
