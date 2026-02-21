package com.fulfillment.orderservice.infrastructure.client.warehouse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fulfillment.orderservice.domain.ports.WarehouseClient;

@Component
public class WarehouseHttpClient implements WarehouseClient{
    
    private final RestClient client;
    
    public WarehouseHttpClient(@Value("${warehouse.service.base-url}")String baseUrl) {
        this.client = RestClient.builder()
                    .baseUrl(baseUrl)
                    .build();
    }

    @Override
    public boolean anyWarehouseExists() {
        ResponseEntity<Void> resp = client.head()
                .uri("/api/v1/warehouses")
                .retrieve()
                .toBodilessEntity();

        return resp.getStatusCode().value() == 200;

    }

}
