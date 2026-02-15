package com.fulfillment.warehouseservice.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWarehouseRequest(
    @NotBlank String city
) {}
