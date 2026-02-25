package com.fulfillment.orderstateprocesor.infrastructure.client.warehouse;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fulfillment.orderstateprocesor.domain.ports.WarehouseClient;
import com.fulfillment.orderstateprocesor.infrastructure.client.warehouse.dto.WarehouseListResponse;

@Component
public class WarehouseHttpClient implements WarehouseClient {

    private final RestClient client;

    public WarehouseHttpClient(@Value("${services.warehouse.baseUrl}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public boolean existsById(String warehouseId) {
        ResponseEntity<Void> response = client.head()
            .uri("/api/v1/warehouses/{id}", warehouseId)
            .exchange((req, res) -> ResponseEntity.status(res.getStatusCode()).build());

        return response.getStatusCode().is2xxSuccessful();
    }

    @Override
    public List<WarehouseSummary> listWarehouses() {
        var arr = client.get()
            .uri("/api/v1/warehouses")
            .retrieve()
            .body(WarehouseListResponse[].class);

        if (arr == null) return List.of();
        return Arrays.stream(arr)
            .filter(w -> w.lat() != null && w.lng() != null)
            .map(w -> new WarehouseSummary(w.warehouseId(), w.lat(), w.lng()))
            .toList();
    }
}
