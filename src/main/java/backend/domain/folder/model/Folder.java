package backend.domain.folder.model;

import backend.domain.common.AggregateRoot;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Getter
@Slf4j
public class Folder extends AggregateRoot<FolderId> {

    private Long workspaceId;
    private String folderName;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private Folder(FolderId id,Long workspaceId, String folderName, boolean isDeleted, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.folderName = folderName;
        this.isDeleted = isDeleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getIdValue() {
        return this.id != null ? this.id.value() : null;
    }
}