package com.fulfillment.warehouseservice.api.config;

import com.fulfillment.warehouseservice.domain.gateway.WarehouseGateway;
import com.fulfillment.warehouseservice.usecase.operation.WarehouseUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public WarehouseUseCase warehouseUseCase(WarehouseGateway warehouseGateway) {
        return new WarehouseUseCase(warehouseGateway);
    }
}
