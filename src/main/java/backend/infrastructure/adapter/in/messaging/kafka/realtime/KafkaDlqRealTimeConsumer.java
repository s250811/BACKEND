package backend.infrastructure.adapter.in.messaging.kafka.realtime;

import backend.application.port.in.realtime.RealTimeUseCase;
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

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaDlqRealTimeConsumer {

    private final RealTimeUseCase realTimeUseCase;
    private final ObjectMapper objectMapper;

    @Bean
    public Function<Flux<Message<String>>, Mono<Void>> realTimeDlqConsumer() {
        return flux -> flux
                .flatMap(message -> {
                    Event event;
                    try {
                        event = objectMapper.readValue(message.getPayload(), Event.class);
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                    Long workspaceId = message.getHeaders().get("workspaceId", Long.class);

                    log.info("Real-Time DLQ 재처리 시작: eventId={}, workspaceId={}", event.getId(), workspaceId);

                    return realTimeUseCase.processEvent(event, workspaceId)
                            .doOnSuccess(v -> log.info("Real-Time DLQ 재처리 성공: eventId={}", event.getId()))
                            .onErrorResume(error -> {
                                log.error("Real-Time DLQ 재처리 실패 (수동 재발행 필요): eventId={}",
                                        event.getId(), error);
                                // DLQ의 DLQ는 없으므로 메시지 제거 (Ack)
                                // 무한 재시도 방지 - 로그 확인 후 수동 재발행 필요
                                return Mono.empty();
                            });
                })
                .then();
    }
}