package backend.domain.project.model;

import backend.domain.common.AggregateRoot;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Getter
public class Project extends AggregateRoot<ProjectId> {

    private Long folderId;

    private String projectName;

    private String description;

    private boolean isDeleted;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public Project(ProjectId id, Long folderId, String projectName, String description,
                   boolean isDeleted, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.folderId = folderId;
        this.projectName = projectName;
        this.description = description;
        this.isDeleted = isDeleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getIdValue() {
        return this.id != null ? this.id.getValue() : null;
    }

}
