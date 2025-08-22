package backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final R2dbcEntityTemplate r2dbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        initializeDatabase().subscribe();
    }

    private Mono<Void> initializeDatabase() {
        return Mono.fromCallable(() -> {
                    ClassPathResource resource = new ClassPathResource("sql/schema.sql");
                    return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                })
                .flatMap(sql -> r2dbcTemplate.getDatabaseClient()
                        .sql(sql)
                        .then())
                .doOnSuccess(unused -> log.info("Database schema initialized successfully"))
                .doOnError(error -> log.error("Failed to initialize database schema", error));
    }
}