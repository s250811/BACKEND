package backend.domain.comment.model;


import backend.domain.common.ValueObject;
import lombok.Value;

@Value
public class CommentId extends ValueObject {
    Long value;
    public static CommentId of(Long id) {
        return new CommentId(id);
    }
}

