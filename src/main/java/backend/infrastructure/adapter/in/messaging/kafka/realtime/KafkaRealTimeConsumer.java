package backend.infrastructure.adapter.in.messaging.kafka.realtime;

import backend.application.port.in.realtime.RealTimeUseCase;
import backend.domain.event.Event;
import backend.application.port.in.realtime.RealTimeStreamUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaRealTimeConsumer {

    private final RealTimeUseCase realTimeUseCase;

    @KafkaListener(topics = "${spring.kafka.topics.real-time-events}", groupId = "${spring.kafka.consumer.group-id}")
    public Mono<Void> handleWorkspaceEvent(
            @Payload Event<?> event,
            @Header("workspaceId") Long workspaceId) {

        log.info("Received Kafka event: {} for workspace: {}", event.getId(), workspaceId);

        return realTimeUseCase.processEvent(event, workspaceId)
                .doOnSuccess(v -> log.debug("Successfully processed event: {}", event.getId()))
                .doOnError(err -> log.error("Failed to process event: {}", event.getId(), err));
    }
}
