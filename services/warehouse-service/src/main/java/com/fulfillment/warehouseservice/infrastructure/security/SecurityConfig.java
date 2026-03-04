package com.fulfillment.warehouseservice.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth

                .requestMatchers("/health", "/actuator/health").permitAll()

                .requestMatchers("/internal/v1/**").permitAll()

                .requestMatchers(HttpMethod.POST, "/api/v1/warehouses").hasRole("ADMIN")

                .requestMatchers(HttpMethod.POST,
                    "/api/v1/warehouses/*/orders/*/picking/start",
                    "/api/v1/warehouses/*/orders/*/packing/start"
                ).hasAnyRole("WAREHOUSE_MANAGER", "ADMIN")

                .requestMatchers(HttpMethod.GET, "/api/v1/warehouses/**")
                    .hasAnyRole("WAREHOUSE_MANAGER", "OPERATOR", "ADMIN")

                .requestMatchers("/api/**").authenticated()

                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));

        return http.build();
    }
}
