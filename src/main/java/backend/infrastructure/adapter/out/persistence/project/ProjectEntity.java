package backend.infrastructure.adapter.out.persistence.project;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "\"project\"")
public class ProjectEntity implements Persistable<Long> {

    @Id
    private Long id;

    private Long folderId;

    private String projectName;

    private String description;

    private boolean isDeleted;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Override
    @Transient
    public boolean isNew() {
        return this.createdAt == null;
    }
}