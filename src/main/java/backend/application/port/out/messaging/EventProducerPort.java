package backend.application.port.out.messaging;

import backend.domain.common.ValueObject;
import backend.domain.event.Event;
import reactor.core.publisher.Mono;

import java.io.Serializable;

public interface EventProducerPort {
    <T extends ValueObject & Serializable> Mono<Void> publishEvent(Event<T> event);
}