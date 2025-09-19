package backend.infrastructure.config;

import backend.infrastructure.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableR2dbcRepositories(basePackages = "backend.infrastructure.adapter.out.persistence")
@EnableR2dbcAuditing
@EnableTransactionManagement
@Slf4j
public class R2dbcConfig {
    @Bean
    public ReactiveAuditorAware<Long> auditorProvider() {
        return SecurityUtils::getCurrentUserId;
    }
}

