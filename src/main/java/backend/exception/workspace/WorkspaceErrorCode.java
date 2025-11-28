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
    WORKSPACE_CREATE_UPDATE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "워크스페이스 생성/수정 중 오류가 발생했습니다."),

    // Member
    NOT_WORKSPACE_MEMBER(HttpStatus.FORBIDDEN, "워크스페이스 멤버가 아닙니다."),
    WORKSPACE_OWNER_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "워크스페이스에 소유자가 존재하지 않습니다."),
    USER_ALREADY_WORKSPACE_MEMBER(HttpStatus.CONFLICT, "사용자가 이미 워크스페이스 멤버입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
