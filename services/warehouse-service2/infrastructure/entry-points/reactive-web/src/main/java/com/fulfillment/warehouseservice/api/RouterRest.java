package com.fulfillment.warehouseservice.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class RouterRest {

    @Bean
    public RouterFunction<ServerResponse> warehouseRoutes(Handler handler) {
        return RouterFunctions.route()
                .POST("/api/v1/warehouses", handler::create)
                .GET("/api/v1/warehouses/{id}", handler::getById)
                .DELETE("/api/v1/warehouses/{id}", handler::delete)
                .build();
    }
}
