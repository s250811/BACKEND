package backend.domain.common;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public abstract class AggregateRoot<ID> {
    protected ID id;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;
}
