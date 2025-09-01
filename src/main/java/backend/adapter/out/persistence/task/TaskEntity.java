package backend.adapter.out.persistence.task;

import backend.domain.task.model.TaskStatus;
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
    private String fileUrl;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

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
