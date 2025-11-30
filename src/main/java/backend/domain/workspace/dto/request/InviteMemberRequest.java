package backend.domain.workspace.dto.request;

import jakarta.validation.constraints.NotNull;

public record InviteMemberRequest(
        @NotNull Long workspaceId,
        @NotNull Long inviteeId
){}