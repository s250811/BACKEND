package backend.domain.common;


import java.io.Serializable;

public interface ValueObject extends Serializable {
    Long value();
}

