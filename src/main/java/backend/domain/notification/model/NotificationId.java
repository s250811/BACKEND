package backend.domain.notification.model;

import backend.domain.common.ValueObject;
import lombok.Value;

@Value
public class NotificationId extends ValueObject {
    Long value;
    public static NotificationId of(Long id) {
        return new NotificationId(id);
    }
}
