package backend.domain.workspaceMember.model;

import backend.domain.common.AggregateRoot;
import backend.domain.user.model.UserId;
import backend.domain.workspace.model.WorkspaceId;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class WorkspaceMember extends AggregateRoot<WorkspaceMemberId> {

    private WorkspaceId workspaceId;
    private UserId userId;
    private String nickname;
    private String description;
    private WorkspaceMemberRole role;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public WorkspaceMember(WorkspaceMemberId id, WorkspaceId workspaceId, UserId userId,
                           String nickname, String description, WorkspaceMemberRole role,
                           boolean isDeleted, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.nickname = nickname;
        this.description = description;
        this.role = role;
        this.isDeleted = isDeleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static WorkspaceMember createOwner(UserId id, WorkspaceId id1) {
        return WorkspaceMember.builder()
                .userId(id)
                .workspaceId(id1)
                .role(WorkspaceMemberRole.OWNER)
                .isDeleted(false)
                .build();
    }

    public Long getIdValue() {
        return this.id != null ? this.id.getValue() : null;
    }

}
