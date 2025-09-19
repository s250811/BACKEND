package backend.domain.event.impl;

import backend.domain.comment.model.Comment;
import backend.domain.event.Event;
import backend.domain.event.EventType;
import lombok.Builder;

public class CommentUpdatedEvent extends Event<Comment> {
    @Builder
    public CommentUpdatedEvent(Comment param) {
        super(EventType.TASK_UPDATED, param);
    }
    @Override
    public String getPartitionKey() {
        return getParam().getId().getValue().toString();
    }
}
