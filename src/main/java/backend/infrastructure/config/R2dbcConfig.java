package backend.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableR2dbcRepositories(basePackages = "backend.infrastructure.adapter.out.persistence")
@EnableR2dbcAuditing
@EnableTransactionManagement
public class R2dbcConfig {
}

