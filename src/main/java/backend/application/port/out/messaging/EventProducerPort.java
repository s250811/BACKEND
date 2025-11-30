package backend.application.port.out.messaging;

import backend.domain.common.AggregateRoot;
import backend.domain.common.ValueObject;
import backend.domain.event.Event;
import reactor.core.publisher.Mono;

import java.io.Serializable;

public interface EventProducerPort {
    <ID extends ValueObject, T extends AggregateRoot<ID> & Serializable> Mono<Void> publishEvent(Event<T> event);
}