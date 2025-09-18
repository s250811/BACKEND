package backend.domain.event;

import backend.domain.common.AggregateRoot;
import backend.domain.common.SnowflakeIdGenerator;
import lombok.*;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Event<T extends Serializable> extends AggregateRoot<EventId> implements Serializable {
    private EventType type;
    private T param;

    private EventTopic topic;

    private List<EventTopic> topics;


    public Event(EventType type, T param) {
        this.id = EventId.of(SnowflakeIdGenerator.nextId());
        this.type = type;
        this.param = param;
    }

    public Event(EventType type, T param, EventTopic topic) {
        this.id = EventId.of(SnowflakeIdGenerator.nextId());
        this.type = type;
        this.param = param;
        this.topic = topic;
    }

    public Event(EventType type, T param, List<EventTopic> topics) {
        this.id = EventId.of(SnowflakeIdGenerator.nextId());
        this.type = type;
        this.param = param;
        this.topics = topics;
    }

    public List<String> getTopics() {
        if (topics != null && !topics.isEmpty()) {
            return topics.stream()
                    .filter(Objects::nonNull)
                    .map(EventTopic::getName)
                    .collect(Collectors.toList());
        }
        if (topic != null) {
            return Collections.singletonList(topic.getName());
        }
        return Collections.emptyList();
    }


    public abstract String getPartitionKey();
}