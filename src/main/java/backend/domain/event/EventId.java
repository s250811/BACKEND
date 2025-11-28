package backend.domain.event;

import backend.domain.common.ValueObject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;


public record EventId(Long value) implements ValueObject {
    @JsonCreator
    public EventId(@JsonProperty("value") Long value) {
        this.value = value;
    }
    public static EventId of(Long id) {
        return new EventId(id);
    }
}


