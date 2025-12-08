package backend.application.port.out.task;

import reactor.core.publisher.Mono;

public interface TaskHistoryPort {
    Mono<Void> syncTaskHistoryToAi();
}
