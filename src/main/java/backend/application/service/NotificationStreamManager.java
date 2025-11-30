package backend.application.service;

import backend.application.port.in.notification.NotificationStreamUseCase;
import backend.domain.notification.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Component
public class NotificationStreamManager implements NotificationStreamUseCase {

    private final Map<Long, Sinks.Many<Notification>> userConnections = new ConcurrentHashMap<>();

    @Override
    public Flux<Notification> createConnection(Long userId) {
        removeConnection(userId);

        Sinks.Many<Notification> sink = Sinks.many().multicast().onBackpressureBuffer();
        userConnections.put(userId, sink);
        log.info("SSE 연결 생성: userId={}", userId);

        return sink.asFlux()
                .doOnCancel(() -> removeConnection(userId))
                .doOnComplete(() -> removeConnection(userId))
                .doOnError(error -> {
                    log.error("SSE 오류: userId={}", userId, error);
                    removeConnection(userId);
                });
    }

    @Override
    public void removeConnection(Long userId) {
        Sinks.Many<Notification> sink = userConnections.remove(userId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }

    @Override
    public int getActiveConnectionCount() {
        return userConnections.size();
    }

    @Override
    public boolean isConnected(Long userId) {
        return userConnections.containsKey(userId);
    }

    @Override
    public Mono<Void> sendToUser(Long userId, Notification notification) {
        Sinks.Many<Notification> sink = userConnections.get(userId);

        if (sink == null) {
            log.debug("SSE 연결 없음: userId={}", userId);
            return Mono.empty();
        }

        Sinks.EmitResult result = sink.tryEmitNext(notification);

        if (result.isFailure()) {
            log.warn("SSE 전송 실패: userId={}, result={}", userId, result);
            removeConnection(userId);
        }

        return Mono.empty();
    }
}