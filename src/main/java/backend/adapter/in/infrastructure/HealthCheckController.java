package backend.adapter.in.infrastructure;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Optional;

@RestController
public class HealthCheckController {

    // jpa DataBaseEntity id create
    private final BoardRepository dataBaseRepository;

    public HealthCheckController(BoardRepository dataBaseRepository) {
        this.dataBaseRepository = dataBaseRepository;
    }

    @GetMapping("/health")
    public Mono<String> healthCheck() {
        return Mono.just("OK");
    }

    @PostMapping("/health/create")
    public Mono<String> readinessCheck() {
        BoardEntity testName = BoardEntity.builder()
                .name("Test Name")
                .build();
        dataBaseRepository.save(testName);

        return Mono.just("create");
    }

    @GetMapping("/health/get")
    public Mono<String> getDataBaseEntity() {

        Optional<BoardEntity> first = dataBaseRepository.findAll()
                .stream()
                .findFirst();

        return Mono.justOrEmpty(first.get().getName());
    }
}
