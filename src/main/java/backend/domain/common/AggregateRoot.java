package backend.domain.common;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public abstract class AggregateRoot<ID> {
    protected ID id;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;
}
