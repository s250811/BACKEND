package backend.exception.workspace;

import backend.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WorkspaceErrorCode implements ErrorCode {

    // Workspace
    WORKSPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "워크스페이스를 찾을 수 없습니다."),
    WORKSPACE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "워크스페이스에 대한 접근 권한이 없습니다."),
    WORKSPACE_OWNER_REQUIRED(HttpStatus.FORBIDDEN, "워크스페이스 소유자 권한이 필요합니다."),
    USER_ALREADY_OWNS_WORKSPACE(HttpStatus.CONFLICT, "사용자가 이미 워크스페이스를 소유하고있습니다."),

    // Folder
    FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "폴더를 찾을 수 없습니다."),
    FOLDER_NOT_IN_WORKSPACE(HttpStatus.BAD_REQUEST, "폴더가 해당 워크스페이스에 속해있지 않습니다."),

    // Project
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."),
    PROJECT_NOT_IN_FOLDER(HttpStatus.BAD_REQUEST, "프로젝트가 해당 폴더에 속해있지 않습니다."),

    // Task
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "태스크를 찾을 수 없습니다."),
    PARENT_TASK_INVALID(HttpStatus.BAD_REQUEST, "상위 태스크가 같은 프로젝트에 속해있지 않습니다."),
    TASK_NOT_IN_PROJECT(HttpStatus.BAD_REQUEST, "태스크가 해당 프로젝트에 속해있지 않습니다."),

    // Member
    NOT_WORKSPACE_MEMBER(HttpStatus.FORBIDDEN, "워크스페이스 멤버가 아닙니다."),
    WORKSPACE_OWNER_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "워크스페이스에 소유자가 존재하지 않습니다."),
    USER_ALREADY_WORKSPACE_MEMBER(HttpStatus.CONFLICT, "사용자가 이미 워크스페이스 멤버입니다."),

    // Hierarchy Validation
    PARENT_TASK_DIFFERENT_PROJECT(HttpStatus.UNPROCESSABLE_ENTITY,"부모 태스크와 자식 태스크는 동일한 프로젝트에 속해야 합니다");

    private final HttpStatus httpStatus;
    private final String message;
}
