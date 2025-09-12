package backend.application.service;

import backend.domain.notification.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseNotificationService {

    private final Map<Long, Sinks.Many<Notification>> userConnections = new ConcurrentHashMap<>();

    public Flux<Notification> createNotificationStream(Long userId) {
        removeConnection(userId);
        Sinks.Many<Notification> sink = Sinks.many().multicast().onBackpressureBuffer();
        userConnections.putIfAbsent(userId, sink);

        log.info("SSE 알림 스트림 생성됨 - 사용자 ID: {}", userId);

        return sink.asFlux()
                .doOnCancel(() -> removeConnection(userId))
                .doOnComplete(() -> removeConnection(userId))
                .doOnError(error -> {
                    log.error("SSE 스트림 에러 발생 - 사용자 ID: {}, 에러: {}", userId, error.getMessage());
                    removeConnection(userId);
                });
    }

    public void removeConnection(Long userId) {
        Sinks.Many<Notification> sink = userConnections.remove(userId);
        if (sink != null) {
            sink.tryEmitComplete();
            log.info("SSE 연결 종료됨 - 사용자 ID: {}", userId);
        }
    }

    public Mono<Void> sendNotificationToUser(Long userId, Notification notification) {
        Sinks.Many<Notification> sink = userConnections.get(userId);

        if (sink == null) {
            log.debug("SSE 연결이 존재하지 않음 - 사용자 ID: {}", userId);
            return Mono.empty();
        }

        Sinks.EmitResult result = sink.tryEmitNext(notification);

        if (result.isFailure()) {
            log.warn("알림 전송 실패 - 사용자 ID: {}, 결과: {}", userId, result);
            if (result == Sinks.EmitResult.FAIL_TERMINATED) {
                removeConnection(userId);
            }
        } else {
            log.debug("알림 전송 성공 - 사용자 ID: {}, 알림 ID: {}", userId, notification.getIdValue());
        }

        return Mono.empty();
    }

    public int getActiveConnectionCount() {
        return userConnections.size();
    }

    public boolean isUserConnected(Long userId) {
        return userConnections.containsKey(userId);
    }
}