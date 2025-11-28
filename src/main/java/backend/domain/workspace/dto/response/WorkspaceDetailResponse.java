package backend.domain.workspace.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 워크스페이스 상세 정보를 나타냅니다.
 * @param workspaceId
 * @param workspaceName
 * @param imageUrl
 * @param description
 * @param owner
 * @param members
 * @param folders
 * @param createdAt
 */
public record WorkspaceDetailResponse (
        Long workspaceId,
        String workspaceName,
        String imageUrl,
        String description,
        MemberInfo owner,
        List<MemberInfo> members,
        List<FolderInfo> folders,
        LocalDateTime createdAt
) {
    /**
     * 워크스페이스 멤버 한 명의 정보를 나타냅니다.
     */
    public record MemberInfo(
            Long userId,
            String nickname,
            String profileImageUrl,
            String role
    ) {}

    /**
     * 폴더와 그 하위 구조(프로젝트, 태스크)에 대한 요약 정보를 나타냅니다.
     */
    public record FolderInfo(
            Long folderId,
            String folderName,
            List<ProjectInfo> projects
    ) {}

    /**
     * 프로젝트와 그 하위 태스크에 대한 요약 정보를 나타냅니다.
     */
    public record ProjectInfo(
            Long projectId,
            String projectName,
            List<TaskInfo> tasks
    ) {}

    /**
     * 태스크에 대한 최소한의 요약 정보를 나타냅니다.
     */
    public record TaskInfo(
            Long taskId,
            Long parentId,
            String taskName,
            String status
    ) {}
}

