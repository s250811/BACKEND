package backend.exception.file;

import backend.exception.ErrorCode;
import backend.exception.ErrorCodeHolder;
import lombok.Getter;

@Getter
public class FileException extends RuntimeException implements ErrorCodeHolder {
    private final ErrorCode errorCode;

    public FileException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
