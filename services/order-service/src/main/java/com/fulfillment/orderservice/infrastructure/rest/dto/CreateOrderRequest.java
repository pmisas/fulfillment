package com.fulfillment.orderservice.infrastructure.rest.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(
    @NotEmpty @Valid List<Item> items,
    @NotNull Double lat,
    @NotNull Double lng

    ) {
    public record Item(
        @NotBlank String sku,
        @Positive int quantity
    ) {}
}
