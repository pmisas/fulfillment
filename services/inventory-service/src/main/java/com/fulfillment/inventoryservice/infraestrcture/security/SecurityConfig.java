package com.fulfillment.inventoryservice.infraestrcture.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        var converter = new CognitoGroupsConverter();

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                    "/health",
                    "/actuator/health",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**"
                ).permitAll()

                .requestMatchers("/internal/v1/**").permitAll()

                .requestMatchers(HttpMethod.POST, "/api/v1/warehouses/*/inventory/restock")
                    .hasAnyRole("WAREHOUSE_MANAGER", "ADMIN")

                .requestMatchers(HttpMethod.GET, "/api/v1/warehouses/*/inventory")
                    .hasAnyRole("WAREHOUSE_MANAGER", "ADMIN")

                .requestMatchers("/api/**").authenticated()

                .anyRequest().authenticated()
            ).oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
            );

        return http.build();
    }
}
