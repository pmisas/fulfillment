package com.fulfillment.shippingservice.infrastructure.rest.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateShipmentRequest(
        @NotBlank String orderId,
        @NotBlank String warehouseId,
        @NotBlank String carrier,
        @NotEmpty @Valid List<Item> items,
        @NotNull Instant estimatedDeliveryAt) {

    public record Item(
            @NotBlank String sku,
            @Positive int quantity) {
    }
}
