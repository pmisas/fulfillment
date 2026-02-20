package com.fulfillment.inventoryservice.infraestrcture.client.warehouse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fulfillment.inventoryservice.domain.ports.WarehouseClient;

@Component
public class WarehouseHttpClient implements WarehouseClient{
    
    private final RestClient client;
    
    public WarehouseHttpClient(@Value("${warehouse.service.base-url}")String baseUrl) {
        this.client = RestClient.builder()
                    .baseUrl(baseUrl)
                    .build();
    }


    @Override
    public boolean existsById(String warehouseId) {
        ResponseEntity<Void> response = client.head()
            .uri("/api/v1/warehouses/{id}", warehouseId)
            .exchange((request, clientResponse) -> 
                ResponseEntity.status(clientResponse.getStatusCode()).build()
            );

        return response.getStatusCode().is2xxSuccessful();

    }

}
