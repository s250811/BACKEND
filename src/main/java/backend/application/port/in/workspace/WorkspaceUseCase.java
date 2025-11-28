package backend.application.port.in.workspace;

import backend.domain.workspace.dto.request.InviteMemberRequest;
import backend.domain.workspace.dto.request.UpdateWorkspaceRequest;
import backend.domain.workspace.dto.response.WorkspaceDetailResponse;
import reactor.core.publisher.Mono;

// service interface
public interface WorkspaceUseCase {

    Mono<Void> inviteMember(InviteMemberRequest command);
    Mono<WorkspaceDetailResponse> getWorkspaceById(Long workspaceId);
    Mono<Void> createOrUpdateWorkspace(UpdateWorkspaceRequest request);
}
