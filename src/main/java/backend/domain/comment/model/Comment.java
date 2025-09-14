package backend.domain.comment.model;

import backend.domain.common.AggregateRoot;
import backend.domain.task.model.TaskId;
import backend.domain.user.model.UserId;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class Comment extends AggregateRoot<CommentId> implements Serializable {
    private TaskId taskId;
    private String content;
    private String fileUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserId lastModifiedBy;

    @Builder
    public Comment(CommentId id, TaskId taskId, String content, String fileUrl,
                   LocalDateTime createdAt, LocalDateTime updatedAt, UserId lastModifiedBy) {
        this.id = id;
        this.taskId = taskId;
        this.content = content;
        this.fileUrl = fileUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastModifiedBy = lastModifiedBy;
    }

    public Long getIdValue() {
        return this.id != null ? this.id.getValue() : null;
    }
    public Long getLastModifiedBy() {
        return this.lastModifiedBy != null ? this.lastModifiedBy.getValue() : null;
    }
    public void updateContent(String newContent) {
        this.content = newContent;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateFileUrl(String newFileUrl) {
        this.fileUrl = newFileUrl;
        this.updatedAt = LocalDateTime.now();
    }
    public List<Long> extractMentionedUserIds() {
        if (content == null || content.isEmpty()) {
            return List.of();
        }
        Pattern pattern = Pattern.compile("@mention\\[(\\d+)\\]");
        Matcher matcher = pattern.matcher(content);

        return matcher.results()
                .map(matchResult -> Long.parseLong(matchResult.group(1)))
                .distinct()
                .toList();
    }
    public boolean hasMentions() {
        return !extractMentionedUserIds().isEmpty();
    }
}

