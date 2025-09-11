package backend.domain.event.audit;

import backend.domain.common.ValueObject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EventAuditId extends ValueObject {
    Long value;
    @JsonCreator
    public EventAuditId(Long value) {
        this.value = value;
    }
    public static EventAuditId of(Long id) {
        return new EventAuditId(id);
    }
}


