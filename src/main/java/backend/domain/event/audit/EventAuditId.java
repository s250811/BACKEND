package backend.domain.event.audit;

import backend.domain.common.ValueObject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

public record EventAuditId(Long value) implements ValueObject {
    @JsonCreator
    public EventAuditId(@JsonProperty("value") Long value) {
        this.value = value;
    }
    public static EventAuditId of(Long value) {
        return new EventAuditId(value);
    }
}


