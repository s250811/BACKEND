package backend.domain.user.model;


import backend.domain.common.ValueObject;
import lombok.Value;

import java.util.UUID;

@Value
public class UserId extends ValueObject {
    UUID value;

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }
    public static UserId of(UUID id) {
        return new UserId(id);
    }
}

