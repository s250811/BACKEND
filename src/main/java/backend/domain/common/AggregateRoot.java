package backend.domain.common;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public abstract class AggregateRoot<ID extends ValueObject> {  // <-- 이 부분 추가!
    protected ID id;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;
}
