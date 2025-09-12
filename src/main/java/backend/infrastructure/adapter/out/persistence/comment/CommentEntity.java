package backend.infrastructure.adapter.out.persistence.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "comment")
public class CommentEntity implements Persistable<Long> {
    @Id
    private Long id;
    private Long taskId;
    @LastModifiedBy
    private Long userId;
    private String content;
    private String fileUrl;
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

