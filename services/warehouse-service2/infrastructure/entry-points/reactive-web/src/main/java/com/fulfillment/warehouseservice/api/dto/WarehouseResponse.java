package com.fulfillment.warehouseservice.api.dto;

import com.fulfillment.warehouseservice.domain.model.Warehouse;
import java.time.Instant;

public record WarehouseResponse(
        String warehouseId,
        String city,
        double lat,
        double lng,
        Instant createdAt
) {

    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getWarehouseId(),
                warehouse.getCity(),
                warehouse.getLat(),
                warehouse.getLng(),
                warehouse.getCreatedAt()
        );
    }
}
