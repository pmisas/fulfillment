package com.fulfillment.orderstateprocesor.infrastructure.client.shipping;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fulfillment.orderstateprocesor.domain.ports.ShippingClient;
import com.fulfillment.orderstateprocesor.infrastructure.client.shipping.dto.CreateShipmentRequest;

import reactor.core.publisher.Mono;

@Component
public class ShippingHttpClient implements ShippingClient {

    private static final Logger log = LoggerFactory.getLogger(ShippingHttpClient.class);

    private final WebClient webClient;

    public ShippingHttpClient(@Value("${services.shipping.baseUrl}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public Mono<Void> createShipment(String orderId, String warehouseId, List<ShipmentItemDto> items) {
        List<CreateShipmentRequest.Item> dtoItems = items.stream()
                .map(i -> new CreateShipmentRequest.Item(i.sku(), i.quantity()))
                .toList();

        log.info("HTTP POST /internal/v1/shipments for orderId={}", orderId);

        return webClient.post()
                .uri("/internal/v1/shipments")
                .bodyValue(new CreateShipmentRequest(orderId, warehouseId, dtoItems))
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(r -> log.info("Shipment created for orderId={}", orderId))
                .doOnError(ex -> log.error("Failed to create shipment for orderId={}: {}", orderId, ex.getMessage()))
                .then();
    }
}
