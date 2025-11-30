package backend.exception.workspace;

import backend.exception.ErrorCode;
import backend.exception.ErrorCodeHolder;
import lombok.Getter;

@Getter
public class WorkspaceException extends RuntimeException implements ErrorCodeHolder {
    private final ErrorCode errorCode;

    public WorkspaceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
