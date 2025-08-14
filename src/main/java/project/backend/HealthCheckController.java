package project.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class HealthCheckController {

    @GetMapping("/health")
    public Mono<String> healthCheck() {
        return Mono.just("OK12");
    }
}
