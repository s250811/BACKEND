package backend.application.port.in;

import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

// service interface
public interface WorkspaceUseCase {

    record CreateWorkspaceCommand(
            String workspaceName,
            String workspaceUrl,
            String description
    ){}

    record InviteMemberCommand(
            Long workspaceId,
            Long inviteeId
    ){}

    /**
     * 조회된 워크스페이스의 모든 상세 정보를 담는 Result 객체입니다.
     * 워크스페이스의 기본 정보, 멤버, 그리고 폴더/프로젝트/태스크의 전체 계층 구조를 포함합니다.
     */
    record GetWorkspaceResult(
            Long workspaceId,
            String workspaceName,
            String imageUrl,
            String description,
            MemberInfo owner,
            List<MemberInfo> members,
            List<FolderInfo> folders,
            LocalDateTime createdAt
    ) {}

    /**
     * 워크스페이스 멤버 한 명의 정보를 나타냅니다.
     */
    record MemberInfo(
            Long userId,
            String nickname,
            String profileImageUrl,
            String role
    ) {}

    /**
     * 폴더와 그 하위 구조(프로젝트, 태스크)에 대한 요약 정보를 나타냅니다.
     */
    record FolderInfo(
            Long folderId,
            String folderName,
            List<ProjectInfo> projects
    ) {}

    /**
     * 프로젝트와 그 하위 태스크에 대한 요약 정보를 나타냅니다.
     */
    record ProjectInfo(
            Long projectId,
            String projectName,
            List<TaskInfo> tasks
    ) {}

    /**
     * 태스크에 대한 최소한의 요약 정보를 나타냅니다.
     */
    record TaskInfo(
            Long taskId,
            Long parentId,
            String taskName,
            String status
    ) {}

    Mono<Void> createWorkspace(CreateWorkspaceCommand command);
    Mono<Void> inviteMember(InviteMemberCommand command);

    Mono<GetWorkspaceResult> getWorkspaceById(Long workspaceId);
}
