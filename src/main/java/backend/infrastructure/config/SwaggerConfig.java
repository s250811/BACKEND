package backend.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI SwaggerConfig() {
        return new OpenAPI()
                .info(new Info()
                        .title("Backend API")
                        .version("1.0.0")
                        .description("누구나 PM이 될 수 있다 API 명세서"))
                .servers(List.of(
                        new Server().url("http://43.201.194.237:8080"),
                        new Server().url("http://localhost:8080")))
                .addSecurityItem(new SecurityRequirement().addList("Authorization"))
                .components(new Components()
                        .addSecuritySchemes("Authorization",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
