package com.fulfillment.warehouseservice.api.dto;

public record CreateWarehouseRequest(
        String city,
        double lat,
        double lng
) {
}
