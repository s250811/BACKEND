package backend.application.port.in.realtime;

import backend.domain.event.Event;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

public interface RealTimeUseCase {
   Mono<Void> processEvent(Event<?> event, Long workspaceId);
}
