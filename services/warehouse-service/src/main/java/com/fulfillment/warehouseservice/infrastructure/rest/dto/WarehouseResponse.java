package com.fulfillment.warehouseservice.infrastructure.rest.dto;

public record WarehouseResponse(
    String warehouseId,
    String city,
    Double lat,
    Double lng
) {}
