package backend.domain.comment.model;


import backend.domain.common.ValueObject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CommentId(Long value) implements ValueObject {
    @JsonCreator
    public CommentId(@JsonProperty("value") Long value) {
        this.value = value;
    }
    public static CommentId of(Long id) {
        return new CommentId(id);
    }
}

