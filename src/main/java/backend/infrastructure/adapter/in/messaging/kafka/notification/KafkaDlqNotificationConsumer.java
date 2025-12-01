package backend.infrastructure.adapter.in.messaging.kafka.notification;

import backend.application.port.in.notification.NotificationUseCase;
import backend.domain.event.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;


@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaDlqNotificationConsumer {
    private final NotificationUseCase notificationUseCase;
    private final ObjectMapper objectMapper;

    /**
     * DLQ 재처리 Consumer
     * - 이미 3회 실패한 메시지를 재처리
     * - 재처리 실패 시 메시지 제거 (무한 루프 방지)
     * - 실패 로그 확인 후 수동 재발행 필요
     */
    @Bean
    public Function<Flux<Message<String>>, Mono<Void>> notificationDlqConsumer() {
        return flux -> flux
                .flatMap(message -> {
                    Event event;
                    try {
                        event = objectMapper.readValue(message.getPayload(), Event.class);
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                    log.info("Notification DLQ 재처리 시작: eventId={}", event.getId());

                    return notificationUseCase.processEvent(event)
                            .doOnSuccess(v -> log.info("Notification DLQ 재처리 성공: eventId={}", event.getId()))
                            .onErrorResume(error -> {
                                log.error("Notification DLQ 재처리 최종 실패 (수동 확인 필요): eventId={}",
                                        event.getId(), error);
                                // DLQ의 DLQ는 없으므로 메시지 제거 (Ack)
                                // 무한 재시도 방지 - 로그 확인 후 수동 재발행 필요
                                return Mono.empty();
                            });
                })
                .then();
    }
}