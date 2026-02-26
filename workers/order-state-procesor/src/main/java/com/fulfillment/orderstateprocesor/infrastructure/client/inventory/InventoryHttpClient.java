package com.fulfillment.orderstateprocesor.infrastructure.client.inventory;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fulfillment.orderstateprocesor.domain.ports.InventoryClient;
import com.fulfillment.orderstateprocesor.infrastructure.client.inventory.dto.AvailabilityRequest;
import com.fulfillment.orderstateprocesor.infrastructure.client.inventory.dto.AvailabilityRequest.SkuQuantityDto;
import com.fulfillment.orderstateprocesor.infrastructure.client.inventory.dto.AvailabilityResponse;
import com.fulfillment.orderstateprocesor.infrastructure.client.inventory.dto.ReserveRequest;

@Component
public class InventoryHttpClient implements InventoryClient {

    private final RestClient client;

    public InventoryHttpClient(@Value("${services.inventory.baseUrl}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public ReserveResult reserveAll(String reservationId, String orderId, String warehouseId, List<SkuQuantity> items) {
        List<ReserveRequest.SkuQuantityDto> dtoItems = items.stream()
            .map(i -> new ReserveRequest.SkuQuantityDto(i.sku(), i.quantity()))
            .toList();

        return client.post()
            .uri("/api/v1/warehouses/{warehouseId}/reservations", warehouseId)
            .body(new ReserveRequest(reservationId, orderId, dtoItems))
            .exchange((request, response) -> switch (response.getStatusCode().value()) {
                case 201 -> ReserveResult.RESERVED;
                case 200 -> ReserveResult.ALREADY_RESERVED;
                case 422 -> ReserveResult.INSUFFICIENT_STOCK;
                default  -> throw new IllegalStateException(
                    "Unexpected status from inventory-service: " + response.getStatusCode()
                );
            });
    }

    @Override
    public void releaseReservation(String reservationId) {
        client.delete()
            .uri("/api/v1/reservations/{reservationId}", reservationId)
            .retrieve()
            .toBodilessEntity();
    }

    @Override
    public AvailabilityResult checkAvailability(String warehouseId, List<SkuQuantity> items) {
        List<SkuQuantityDto> dtoItems = items.stream()
            .map(i -> new SkuQuantityDto(i.sku(), i.quantity()))
            .toList();

        AvailabilityResponse response = client.post()
            .uri("/api/v1/warehouses/{warehouseId}/inventory/availability", warehouseId)
            .body(new AvailabilityRequest(dtoItems))
            .retrieve()
            .body(AvailabilityResponse.class);

        if (response == null) return new AvailabilityResult(false, List.of());

        List<ItemAvailability> mapped = response.items().stream()
            .map(i -> new ItemAvailability(i.sku(), i.required(), i.available(), i.canFulfill()))
            .toList();

        return new AvailabilityResult(response.canFulfillAll(), mapped);
    }
}
