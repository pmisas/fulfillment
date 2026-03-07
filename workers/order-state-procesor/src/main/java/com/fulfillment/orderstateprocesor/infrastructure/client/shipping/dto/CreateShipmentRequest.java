package com.fulfillment.orderstateprocesor.infrastructure.client.shipping.dto;

import java.util.List;

public record CreateShipmentRequest(
        String orderId,
        String warehouseId,
        List<Item> items) {

    public record Item(String sku, int quantity) {}
}
