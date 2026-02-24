package com.fulfillment.warehouseservice.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateWarehouseRequest(
    @NotBlank String city,
    @NotNull Double lat,
    @NotNull Double lng
) {}
