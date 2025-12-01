package backend.domain.event;

import backend.domain.common.AggregateRoot;
import backend.domain.common.SnowflakeIdGenerator;
import backend.domain.event.impl.CommentUpdatedEvent;
import backend.domain.event.impl.TaskUpdatedEvent;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;

import java.io.Serializable;

/**
 * Event를 JSON으로 저장/전송할 때 하위 구현체 정보를 함께 기록하여
 * Outbox 및 Kafka Consumer에서 정확한 Event subtype으로 역직렬화되도록 하기 위한 설정
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class"
)
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