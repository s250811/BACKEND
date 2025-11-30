package backend.domain.notification.model;

import backend.domain.common.ValueObject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

public record NotificationId(Long value) implements ValueObject {
    @JsonCreator
    public NotificationId(@JsonProperty("value") Long value) {
        this.value = value;
    }
    public static NotificationId of(Long id) {
        return new NotificationId(id);
    }
}
