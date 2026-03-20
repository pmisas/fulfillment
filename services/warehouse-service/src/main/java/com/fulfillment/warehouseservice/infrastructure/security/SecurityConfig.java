package com.fulfillment.warehouseservice.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
@org.springframework.context.annotation.Profile("cloud")
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

                .requestMatchers(HttpMethod.POST, "/api/v1/warehouses").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/warehouses/*/managers").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/warehouses/*/managers/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/warehouses/*/managers").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/users/*/warehouse-access").hasRole("ADMIN")

                .requestMatchers(HttpMethod.POST,
                    "/api/v1/warehouses/*/orders/*/picking/complete",
                    "/api/v1/warehouses/*/orders/*/packing/complete"
                ).hasAnyRole("WAREHOUSE_MANAGER", "ADMIN")

                .requestMatchers(HttpMethod.GET, "/api/v1/warehouses/**")
                    .hasAnyRole("WAREHOUSE_MANAGER", "OPERATOR", "ADMIN")

                .requestMatchers("/api/**").authenticated()

                .anyRequest().authenticated()
            ).oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
            );

        return http.build();
    }
}
