package backend.application.service;

import backend.application.port.out.event.audit.EventAuditRepositoryPort;
import backend.application.port.out.messaging.EventProducerPort;
import backend.domain.event.Event;
import backend.domain.event.audit.EventAudit;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {
    private final EventAuditRepositoryPort eventAuditRepository;
    private final EventProducerPort eventProducer;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(initialDelay = 60000, fixedDelay = 10000)
    public void publishPendingEvents() {
        if (!running.compareAndSet(false, true)) return; // 이전 작업이 아직 완료되지 않았을 때 중복 실행 방지 위해 락

        eventAuditRepository.findPendingEvents()
                .flatMap(this::lockForProcessing)
                .flatMap(this::publishAndUpdateStatus, 10) // 동시에 최대 10개까지 처리, 나머지는 대기
                .doFinally(signal -> running.set(false)) // 락 해제
                .subscribe();
    }

    private Mono<EventAudit> lockForProcessing(EventAudit eventAudit) {
        eventAudit.markProcessing();
        return eventAuditRepository.save(eventAudit);
    }

     Mono<EventAudit> publishAndUpdateStatus(EventAudit eventAudit) {
        return Mono.fromCallable(() -> objectMapper.readValue(eventAudit.getPayload(), Event.class))
                .subscribeOn(Schedulers.boundedElastic()) // CPU/Blocking 가능한 동기 작업(Json 파싱)을 Reactor EventLoop 밖에서 안전하게 실행

                .flatMap(event ->
                        eventProducer.publishEvent(event)
                        .then(Mono.defer(() -> {
                            eventAudit.markSuccess();
                            return eventAuditRepository.save(eventAudit);
                        }))
                )
                .onErrorResume(ex -> {
                    log.error("이벤트 발행 실패", ex);
                    eventAudit.markFailed(((Throwable) ex).getMessage());
                    return eventAuditRepository.save(eventAudit);
                });
    }
}
