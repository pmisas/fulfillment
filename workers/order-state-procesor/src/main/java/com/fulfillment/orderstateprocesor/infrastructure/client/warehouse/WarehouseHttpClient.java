package com.fulfillment.orderstateprocesor.infrastructure.client.warehouse;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fulfillment.orderstateprocesor.domain.ports.WarehouseClient;
import com.fulfillment.orderstateprocesor.infrastructure.client.warehouse.dto.WarehouseListResponse;

import reactor.core.publisher.Mono;

@Component
public class WarehouseHttpClient implements WarehouseClient {

    private final WebClient webClient;

    public WarehouseHttpClient(@Value("${services.warehouse.baseUrl}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public Mono<Boolean> existsById(String warehouseId) {
        return webClient.head()
            .uri("/api/v1/warehouses/{id}", warehouseId)
            .exchangeToMono(response -> Mono.just(response.statusCode().is2xxSuccessful()));
    }

    @Override
    public Mono<List<WarehouseSummary>> listWarehouses() {
        return webClient.get()
            .uri("/api/v1/warehouses")
            .retrieve()
            .bodyToFlux(WarehouseListResponse.class)
            .filter(w -> w.lat() != null && w.lng() != null)
            .map(w -> new WarehouseSummary(w.warehouseId(), w.lat(), w.lng()))
            .collectList();
    }
}
