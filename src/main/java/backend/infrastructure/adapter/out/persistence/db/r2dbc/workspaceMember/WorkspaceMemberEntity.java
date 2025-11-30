package backend.infrastructure.adapter.out.persistence.db.r2dbc.workspaceMember;

import backend.domain.workspaceMember.model.WorkspaceMemberRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;


@Getter
@Builder
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "\"workspacemember\"")
public class WorkspaceMemberEntity implements Persistable<Long> {

    @Id
    private Long id;

    private Long workspaceId;

    private Long userId;

    private String description;

    private WorkspaceMemberRole role;

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
