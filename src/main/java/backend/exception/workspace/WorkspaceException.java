package backend.exception.workspace;

import backend.exception.ErrorCode;
import lombok.Getter;

@Getter
public class WorkspaceException extends RuntimeException {
    private final ErrorCode errorCode;

    public WorkspaceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
