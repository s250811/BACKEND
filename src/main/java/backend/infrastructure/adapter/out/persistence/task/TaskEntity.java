package backend.infrastructure.adapter.out.persistence.task;

import backend.domain.task.model.TaskStatus;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "\"task\"")
public class TaskEntity implements Persistable<Long> {

    @Id
    private Long id;
    private Long projectId;
    private Long parentId;
    private String taskName;
    private TaskStatus taskStatus;
    private boolean isDeleted;
    private String description;
    @Transient
    private List<Long> managerIds;
    private String fileUrl;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
    @LastModifiedBy
    private Long lastModifiedBy;
    @Override
    @Transient
    public boolean isNew() {
        return this.id == null;
    }
}
