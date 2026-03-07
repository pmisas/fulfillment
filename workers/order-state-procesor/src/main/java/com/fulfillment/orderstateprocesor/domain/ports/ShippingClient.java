package com.fulfillment.orderstateprocesor.domain.ports;

import java.util.List;

import reactor.core.publisher.Mono;

public interface ShippingClient {

    Mono<Void> createShipment(String orderId, String warehouseId, List<ShipmentItemDto> items);
    
    record ShipmentItemDto(String sku, int quantity) {}

}
