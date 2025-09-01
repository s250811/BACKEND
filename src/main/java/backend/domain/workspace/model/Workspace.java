package backend.domain.workspace.model;

import backend.domain.common.AggregateRoot;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class Workspace extends AggregateRoot<WorkspaceId> {

    private String workspaceName;
    private String workspaceImgUrl;
    private String description;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public Workspace(WorkspaceId id, String workspaceName, String workspaceImgUrl, String description, boolean isDeleted, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.workspaceName = workspaceName;
        this.workspaceImgUrl = workspaceImgUrl;
        this.description = description;
        this.isDeleted = isDeleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Workspace create(String workspaceName, String workspaceUrl, String description) {
        return Workspace.builder()
                .workspaceName(workspaceName)
                .workspaceImgUrl(workspaceUrl)
                .description(description)
                .isDeleted(false)
                .build();
    }

    public Long getIdValue() {
        return this.id != null ? this.id.getValue() : null;
    }
}
