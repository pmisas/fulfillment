package com.fulfillment.orderstateprocesor.infrastructure.client.inventory;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fulfillment.orderstateprocesor.domain.ports.InventoryClient;
import com.fulfillment.orderstateprocesor.infrastructure.client.inventory.dto.AvailabilityRequest;
import com.fulfillment.orderstateprocesor.infrastructure.client.inventory.dto.AvailabilityRequest.SkuQuantityDto;
import com.fulfillment.orderstateprocesor.infrastructure.client.inventory.dto.AvailabilityResponse;
import com.fulfillment.orderstateprocesor.infrastructure.client.inventory.dto.ReserveRequest;

import reactor.core.publisher.Mono;

@Component
public class InventoryHttpClient implements InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryHttpClient.class);
    private final WebClient webClient;

    public InventoryHttpClient(@Value("${services.inventory.baseUrl}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public Mono<ReserveResult> reserveAll(String reservationId, String orderId, String warehouseId, List<SkuQuantity> items) {
        List<ReserveRequest.SkuQuantityDto> dtoItems = items.stream()
            .map(i -> new ReserveRequest.SkuQuantityDto(i.sku(), i.quantity()))
            .toList();

        return webClient.post()
            .uri("/internal/v1/warehouses/{warehouseId}/reservations", warehouseId)
            .bodyValue(new ReserveRequest(reservationId, orderId, dtoItems))
            .exchangeToMono(response -> switch (response.statusCode().value()) {
                case 201 -> Mono.just(ReserveResult.RESERVED);
                case 200 -> Mono.just(ReserveResult.ALREADY_RESERVED);
                case 422 -> Mono.just(ReserveResult.INSUFFICIENT_STOCK);
                default  -> Mono.error(new IllegalStateException(
                    "Unexpected status from inventory-service: " + response.statusCode()));
            });
    }

    @Override
    public Mono<Void> releaseReservation(String reservationId) {
        log.info("HTTP DELETE /internal/v1/reservations/{}", reservationId);
        
        return webClient.delete()
            .uri("/internal/v1/reservations/{reservationId}", reservationId)
            .retrieve()
            .toBodilessEntity()
            .doOnSuccess(v -> log.info("HTTP DELETE successful for reservationId={}", reservationId))
            .doOnError(ex -> log.error("HTTP DELETE failed for reservationId={}: {}", reservationId, ex.getMessage(), ex))
            .then();
    }

    @Override
    public Mono<ConsumeResult> consumeReservation(String reservationId) {
        log.info("HTTP POST /internal/v1/reservations/{}/consume", reservationId);

        return webClient.post()
            .uri("/internal/v1/reservations/{reservationId}/consume", reservationId)
            .retrieve()
            .toBodilessEntity()
            .map(r -> ConsumeResult.CONSUMED)
            .onErrorResume(ex -> {
                log.warn("consumeReservation failed for reservationId={}: {}", reservationId, ex.getMessage());
                return Mono.just(ConsumeResult.RESERVATION_NOT_FOUND);
            });
    }

    @Override
    public Mono<AvailabilityResult> checkAvailability(String warehouseId, List<SkuQuantity> items) {
        List<SkuQuantityDto> dtoItems = items.stream()
            .map(i -> new SkuQuantityDto(i.sku(), i.quantity()))
            .toList();

        return webClient.post()
            .uri("/internal/v1/warehouses/{warehouseId}/inventory/availability", warehouseId)
            .bodyValue(new AvailabilityRequest(dtoItems))
            .retrieve()
            .bodyToMono(AvailabilityResponse.class)
            .map(resp -> {
                List<ItemAvailability> mapped = resp.items().stream()
                    .map(i -> new ItemAvailability(i.sku(), i.required(), i.available(), i.canFulfill()))
                    .toList();
                return new AvailabilityResult(resp.canFulfillAll(), mapped);
            })
            .defaultIfEmpty(new AvailabilityResult(false, List.of()));
    }
}
