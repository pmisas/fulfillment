package com.fulfillment.inventoryservice.infraestrcture.rest.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CheckAvailabilityRequest(
    @Valid List<SkuQuantity> items
) {
    public record SkuQuantity(
        @NotBlank String sku, 
        @Positive Integer quantity
    ) {}
}
