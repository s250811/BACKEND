package backend.config;

import backend.security.JwtAuthenticationManager;
import backend.security.JwtServerAuthenticationConverter;
import backend.security.MagicLinkAuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ott.GenerateOneTimeTokenRequest;
import org.springframework.security.authentication.ott.reactive.InMemoryReactiveOneTimeTokenService;
import org.springframework.security.authentication.ott.reactive.ReactiveOneTimeTokenService;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ott.DefaultServerGenerateOneTimeTokenRequestResolver;
import org.springframework.security.web.server.authentication.ott.ServerGenerateOneTimeTokenRequestResolver;

import java.time.Duration;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationManager jwtAuthenticationManager;
    private final JwtServerAuthenticationConverter jwtServerAuthenticationConverter;
    private final MagicLinkAuthenticationSuccessHandler magicLinkAuthSuccessHandler;
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        AuthenticationWebFilter jwtFilter = new AuthenticationWebFilter(jwtAuthenticationManager);
        jwtFilter.setServerAuthenticationConverter(jwtServerAuthenticationConverter);

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/v1/auth/**", "/api/v1/users/").permitAll()
                        .anyExchange().authenticated()
                )
                .oneTimeTokenLogin(ott -> ott.tokenGeneratingUrl("/api/v1/auth/magic-links")
                        .loginProcessingUrl("/api/v1/auth/magic-links/verification")
                        .authenticationSuccessHandler(magicLinkAuthSuccessHandler)
                        .showDefaultSubmitPage(false)
                )
                .build();
    }
    @Bean
    public ReactiveOneTimeTokenService reactiveOneTimeTokenService() {
        return new InMemoryReactiveOneTimeTokenService();
    }

    @Bean
    public ServerGenerateOneTimeTokenRequestResolver generateOneTimeTokenRequestResolver() {
        DefaultServerGenerateOneTimeTokenRequestResolver delegate = new DefaultServerGenerateOneTimeTokenRequestResolver();
        return exchange -> delegate.resolve(exchange)
                .map(request -> new GenerateOneTimeTokenRequest(request.getUsername(), Duration.ofMinutes(10)));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
