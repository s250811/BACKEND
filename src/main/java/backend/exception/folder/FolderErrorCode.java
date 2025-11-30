package backend.exception.folder;

import backend.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FolderErrorCode implements ErrorCode {
    FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "폴더를 찾을 수 없습니다."),
    FOLDER_NOT_IN_WORKSPACE(HttpStatus.BAD_REQUEST, "폴더가 해당 워크스페이스에 속해있지 않습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
