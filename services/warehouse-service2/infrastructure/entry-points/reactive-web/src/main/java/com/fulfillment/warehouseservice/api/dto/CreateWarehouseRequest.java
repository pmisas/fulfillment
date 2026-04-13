package com.fulfillment.warehouseservice.api.commons;

public record CreateWarehouseRequest(
        String city,
        double lat,
        double lng
) {
}
