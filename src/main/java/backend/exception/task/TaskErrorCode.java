package backend.exception.task;

import backend.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TaskErrorCode implements ErrorCode {

    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "태스크를 찾을 수 없습니다."),
    PARENT_TASK_INVALID(HttpStatus.BAD_REQUEST, "상위 태스크가 같은 프로젝트에 속해있지 않습니다."),
    TASK_NOT_IN_PROJECT(HttpStatus.BAD_REQUEST, "태스크가 해당 프로젝트에 속해있지 않습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
