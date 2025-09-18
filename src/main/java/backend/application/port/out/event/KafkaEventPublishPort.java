package backend.application.port.out.event;

import backend.domain.event.Event;
import reactor.core.publisher.Mono;

import java.io.Serializable;

public interface KafkaEventPublishPort {
    <T extends Serializable> Mono<Void> publishEvent(Event<T> event);
}