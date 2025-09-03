package backend.exception.file;

import backend.exception.ErrorCode;

public class FileException extends RuntimeException {
    private final ErrorCode errorCode;

    public FileException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
