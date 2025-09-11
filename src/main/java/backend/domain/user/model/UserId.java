package backend.domain.user.model;


import backend.domain.common.ValueObject;
import lombok.Value;

@Value
public class UserId extends ValueObject {
    Long value;
    public static UserId of(Long id) {
        return new UserId(id);
    }
}

