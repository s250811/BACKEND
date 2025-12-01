package backend.infrastructure.adapter.out.messaging.kafka;

import backend.application.port.out.messaging.EventProducerPort;
import backend.domain.common.AggregateRoot;
import backend.domain.common.ValueObject;
import backend.domain.event.Event;
import backend.domain.event.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventProducer implements EventProducerPort {

    private final StreamBridge streamBridge;

    @Value("${spring.cloud.stream.bindings.realTimeConsumer-in-0.destination}")
    private String realTimeEventsBinding;

    @Value("${spring.cloud.stream.bindings.notificationConsumer-in-0.destination}")
    private String notificationEventsBinding;

    @Override
    public <ID extends ValueObject, T extends AggregateRoot<ID> & Serializable>
    Mono<Void> publishEvent(Event<T> event) {
        List<String> bindings = resolveTargetBindings(event);

        return Flux.fromIterable(bindings)
                .flatMap(binding -> Mono.fromRunnable(() -> {
                    streamBridge.send(binding, event);
                }))
                .then();
    }

    private List<String> resolveTargetBindings(Event<?> event) {
        List<String> bindings = new ArrayList<>();
        bindings.add(realTimeEventsBinding);
        if (event.getType() == EventType.TASK_UPDATED || event.getType() == EventType.COMMENT_UPDATED) {
            bindings.add(notificationEventsBinding);
        }
        return bindings;
    }
}