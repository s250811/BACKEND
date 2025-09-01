package backend.adapter.out.persistence.workspace;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "\"workspace\"")
public class WorkspaceEntity implements Persistable<Long> {

    @Id
    private Long id;

    private String workspaceName;

    @Column("img_url")
    private String workspaceImgUrl;

    private String description;

    private boolean isDeleted;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * save() 메서드 호출 시, insert or update 판단
     * @return
     */
    @Override
    @Transient
    public boolean isNew() {
        return this.createdAt == null;
    }
}
