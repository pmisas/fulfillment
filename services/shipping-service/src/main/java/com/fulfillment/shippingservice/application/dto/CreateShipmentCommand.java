package com.fulfillment.shippingservice.application.dto;

import java.time.Instant;
import java.util.List;

import com.fulfillment.shippingservice.domain.model.CarrierCode;

public record CreateShipmentCommand(
        String orderId,
        String warehouseId,
        CarrierCode carrier,
        List<Item> items,
        Instant estimatedDeliveryAt) {

    public record Item(String sku, int quantity) {
    }
}
