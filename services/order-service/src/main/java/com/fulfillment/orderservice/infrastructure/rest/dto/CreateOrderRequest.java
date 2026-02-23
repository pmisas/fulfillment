package com.fulfillment.orderservice.infrastructure.rest.dto;

import java.util.List;

import io.micrometer.common.lang.NonNull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(
    @NotEmpty @Valid List<Item> items,
    @NonNull Double lat,
    @NonNull Double lng

    ) {
    public record Item(
        @NotBlank String sku,
        @Positive int quantity
    ) {}
}
