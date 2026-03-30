package com.fulfillment.warehouseservice.infrastructure.rest;

import com.fulfillment.warehouseservice.domain.model.Warehouse;
import com.fulfillment.warehouseservice.infrastructure.rest.dto.WarehouseResponse;

public class WarehouseRestMapper {

    public static WarehouseResponse toResponse(Warehouse warehouse) {
        return new WarehouseResponse(
            warehouse.getWarehouseId(),
            warehouse.getCity(),
            warehouse.getLat(),
            warehouse.getLng()
        );
    }
}
