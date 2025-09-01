package backend.domain.workspaceInvite.model;


import backend.domain.common.AggregateRoot;
import backend.domain.user.model.UserId;
import backend.domain.workspace.model.WorkspaceId;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkSpaceInvite extends AggregateRoot<WorkSpaceInviteId> {

    private WorkspaceId workspaceId;
    private UserId fromUserId;
    private UserId toUserId;
    private InvitedStatus status;
}