package backend.infrastructure.adapter.out.messaging.kafka;

import backend.application.port.out.messaging.EventProducerPort;
import backend.domain.common.AggregateRoot;
import backend.domain.common.ValueObject;
import backend.domain.event.Event;
import backend.domain.event.EventType;
import backend.exception.messaging.MessagingErrorCode;
import backend.exception.messaging.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
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
    // consumer binding에 정의된 destination(topic 이름)을 주입받아 StreamBridge에 전달하면 Spring Cloud Stream은 동적으로 producer binding을 생성하여 해당 topic으로 메시지를 전송.
    @Value("${spring.cloud.stream.bindings.realTimeConsumer-in-0.destination}")
    private String realTimeEventsBinding;

    @Value("${spring.cloud.stream.bindings.notificationConsumer-in-0.destination}")
    private String notificationEventsBinding;

    @Override
    public <ID extends ValueObject, T extends AggregateRoot<ID> & Serializable>
    Mono<Void> publishEvent(Event<T> event) {
        T param = event.getParam();
        // Kafka partitioner는 key의 hash를 계산해서 partition을 결정하기 때문에 동일 엔티티의 이벤트 순서 보장하기 위해 엔티티 고유 ID를 Kafka record key로 전달.
        String partitionKey = String.valueOf(param.getId().value());

        List<String> bindings = resolveTargetBindings(event);

        return Flux.fromIterable(bindings)
                .flatMap(binding -> Mono.fromRunnable(() -> {
                    Message<Event<T>> msgWithPartitionKey = MessageBuilder.withPayload(event)
                                    //  KafkaHeaders.KEY는 Kafka ProducerRecord의 key로 매핑.
                                    .setHeader(KafkaHeaders.KEY, partitionKey)
                                    .build();
                    // 반환값(boolean)은 Spring Cloud Stream 내부 채널 전송 성공 여부이며, Kafka 브로커에 브로커에 실제로 기록(ack)되었는지는 보장하지 않음.
                    boolean sentToBinder = streamBridge.send(binding, msgWithPartitionKey);
                    if (!sentToBinder) {
                        throw new MessagingException(MessagingErrorCode.MESSAGE_DISPATCH_FAILED);
                    }
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