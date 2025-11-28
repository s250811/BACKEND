package backend.exception.folder;

import backend.exception.ErrorCode;
import backend.exception.ErrorCodeHolder;
import lombok.Getter;

@Getter
public class FolderException extends RuntimeException implements ErrorCodeHolder {
    private final ErrorCode errorCode;

    public FolderException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
