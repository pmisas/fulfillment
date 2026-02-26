package com.fulfillment.inventoryservice.infraestrcture.rest.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record BatchRequest(
    @NotEmpty @Valid List<SkuQuantity> items) {
    public record SkuQuantity(
        @NotBlank String sku,
        @Min(1) int quantity) {}
}