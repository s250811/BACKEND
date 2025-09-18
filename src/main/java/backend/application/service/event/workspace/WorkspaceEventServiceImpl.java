package backend.application.service.event.workspace;


import backend.application.port.out.event.KafkaEventPublishPort;
import backend.domain.event.Event;
import backend.domain.workspace.model.Workspace;
import backend.infrastructure.config.Workspace.RealtimeEventBroker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkspaceEventServiceImpl implements WorkspaceEventService {
    private final KafkaEventPublishPort eventPublish;
    private final RealtimeEventBroker realtimeEventBroker;

    @Override
    public Mono<Void> publishWorkspaceCreatedEvent(Workspace workspace) {
        if (workspace == null) {
            return Mono.error(new IllegalArgumentException("workspace is null"));
        }

        WorkspaceCreatedEvent event = WorkspaceCreatedEvent.create(workspace);

        // 1) Kafka에 발행
        Mono<Void> kafkaPublish = eventPublish.publishEvent(event)
                .then();

        // 2) 워크스페이스 실시간 스트림에도 발행
        Mono<Void> realtimePublish = Mono.justOrEmpty(workspace.getId())
                .flatMap(workspaceId -> {
                    Map<String, Object> wsPayload = createRealtimePayload(event, workspaceId.getValue());
                    return realtimeEventBroker.publishEvent(workspaceId.getValue(), wsPayload);
                })
                .then();

        return Mono.when(kafkaPublish, realtimePublish)
                .then()
                .doOnSuccess(v -> log.info("워크스페이스 생성 이벤트 발행 완료: workspaceId={}", workspace.getId()))
                .doOnError(error -> log.error("워크스페이스 생성 이벤트 발행 실패: workspaceId={}", workspace.getId(), error));
    }

    @Override
    public Mono<Void> publishWorkspaceUpdatedEvent(Workspace workspace) {
        if (workspace == null) {
            return Mono.error(new IllegalArgumentException("workspace is null"));
        }

        WorkspaceUpdatedEvent event = WorkspaceUpdatedEvent.create(workspace);

        // 1) Kafka에 발행
        Mono<Void> kafkaPublish = eventPublish.publishEvent(event)
                .then();

        // 2) 워크스페이스 실시간 스트림에도 발행
        Mono<Void> realtimePublish = Mono.justOrEmpty(workspace.getId())
                .flatMap(workspaceId -> {
                    Map<String, Object> wsPayload = createRealtimePayload(event, workspaceId.getValue());
                    return realtimeEventBroker.publishEvent(workspaceId.getValue(), wsPayload);
                })
                .then();

        return Mono.when(kafkaPublish, realtimePublish)
                .then()
                .doOnSuccess(v -> log.info("워크스페이스 수정 이벤트 발행 완료: workspaceId={}", workspace.getId()))
                .doOnError(error -> log.error("워크스페이스 수정 이벤트 발행 실패: workspaceId={}", workspace.getId(), error));
    }

    private Map<String, Object> createRealtimePayload(Event<Workspace> event, Long workspaceId) {
        Map<String, Object> payload = new HashMap<>();
        String eventId = (event.getId() != null && event.getId().getValue() != null)
                ? String.valueOf(event.getId().getValue())
                : java.util.UUID.randomUUID().toString();

        payload.put("eventId", eventId);
        payload.put("type", event.getType() != null ? event.getType().name() : "UNKNOWN");
        payload.put("workspaceId", workspaceId);
        payload.put("payload", event.getParam());
        payload.put("timestamp", LocalDateTime.now().toString());
        payload.put("source", "direct");
        return payload;
    }
}
