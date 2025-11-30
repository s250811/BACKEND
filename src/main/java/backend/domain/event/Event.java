package backend.domain.event;

import backend.domain.common.AggregateRoot;
import backend.domain.common.SnowflakeIdGenerator;
import lombok.*;

import java.io.Serializable;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Event<T extends Serializable> extends AggregateRoot<EventId> implements Serializable {
    private EventType type;
    private T param;

    public Event(EventType type, T param) {
        this.id = EventId.of(SnowflakeIdGenerator.nextId());
        this.type = type;
        this.param = param;
    }
}