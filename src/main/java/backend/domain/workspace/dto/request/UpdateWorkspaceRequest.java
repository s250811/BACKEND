package backend.domain.workspace.dto.request;

public record UpdateWorkspaceRequest(
        Long workspaceId,
        String workspaceName,
        String workspaceUrl,
        String description
){
    public boolean isUpdateMode() {
        return workspaceId != null;
    }
}
