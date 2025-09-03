package backend.exception.file;

import backend.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum FileErrorCode implements ErrorCode {

    INVALID_FILENAME(HttpStatus.BAD_REQUEST, "유효하지 않은 파일명입니다."),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
