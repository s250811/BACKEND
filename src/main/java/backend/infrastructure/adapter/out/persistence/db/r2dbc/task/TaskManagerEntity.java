package backend.infrastructure.adapter.out.persistence.db.r2dbc.task;

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
@Table(name = "\"task_manager\"")
public class TaskManagerEntity implements Persistable<Long> {

    @Id
    private Long id;

    private Long taskId;

    private Long userId;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Override
    public boolean isNew() {
        return this.createdAt == null;
    }


}
