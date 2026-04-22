package com.mafia.authservice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

@Configuration
@Profile("dev")
public class SecurityDevConfig {

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        // Keep API security untouched; only exclude Swagger endpoints in dev.
        return web -> web.ignoring().requestMatchers("/swagger-ui/**", "/v3/api-docs/**");
    }
}

