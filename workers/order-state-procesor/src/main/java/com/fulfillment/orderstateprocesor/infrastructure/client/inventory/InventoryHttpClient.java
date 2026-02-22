package com.fulfillment.orderstateprocesor.infrastructure.client.inventory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fulfillment.orderstateprocesor.domain.ports.InventoryClient;
import com.fulfillment.orderstateprocesor.infrastructure.client.inventory.dto.AmountRequest;

@Component
public class InventoryHttpClient implements InventoryClient {

    private final RestClient client;

    public InventoryHttpClient(@Value("${services.inventory.baseUrl}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public void reserve(String warehouseId, String sku, int amount) {
        try {
            client.post()
                .uri("/api/v1/warehouses/{warehouseId}/inventory/{sku}/reserve", warehouseId, sku)
                .body(new AmountRequest(amount))
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw ex;
        }
    }

    @Override
    public void release(String warehouseId, String sku, int amount) {
        try {
            client.post()
                .uri("/api/v1/warehouses/{warehouseId}/inventory/{sku}/release", warehouseId, sku)
                .body(new AmountRequest(amount))
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw ex;
        }
    }
}
