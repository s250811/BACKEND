package backend.domain.common;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public abstract class ValueObject {
    public abstract Long getValue();
}

