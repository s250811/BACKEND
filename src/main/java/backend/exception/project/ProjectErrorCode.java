package backend.exception.project;

import backend.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProjectErrorCode implements ErrorCode {
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."),
    PROJECT_NOT_IN_FOLDER(HttpStatus.BAD_REQUEST, "프로젝트가 해당 폴더에 속해있지 않습니다."),
    PROJECT_DELETE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "프로젝트 삭제 중 오류가 발생했습니다."),
    PROJECT_CREATE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "프로젝트 생성 중 오류가 발생했습니다."),
    PARENT_TASK_DIFFERENT_PROJECT(HttpStatus.UNPROCESSABLE_ENTITY,"부모 태스크와 자식 태스크는 동일한 프로젝트에 속해야 합니다");

    private final HttpStatus httpStatus;
    private final String message;
}
