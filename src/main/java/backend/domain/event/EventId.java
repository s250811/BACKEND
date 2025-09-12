package backend.domain.event;

import backend.domain.common.ValueObject;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EventId extends ValueObject {
    Long value;
    @JsonCreator
    public EventId(Long value) {
        this.value = value;
    }
    public static EventId of(Long id) {
        return new EventId(id);
    }
}


