package backend.domain.user.model;


import backend.domain.common.ValueObject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;


public record UserId(Long value) implements ValueObject {
    @JsonCreator
    public UserId(@JsonProperty("value") Long value) {
        this.value = value;
    }
    public static UserId of(Long value) {
        return new UserId(value);
    }
}

