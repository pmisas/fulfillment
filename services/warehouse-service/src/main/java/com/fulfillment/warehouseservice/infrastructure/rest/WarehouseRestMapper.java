package com.fulfillment.warehouseservice.infrastructure.rest;

import com.fulfillment.warehouseservice.application.dto.CreateWarehouseCommand;
import com.fulfillment.warehouseservice.domain.model.Warehouse;
import com.fulfillment.warehouseservice.infrastructure.rest.dto.CreateWarehouseRequest;
import com.fulfillment.warehouseservice.infrastructure.rest.dto.WarehouseResponse;

public class WarehouseRestMapper {
    
    public static CreateWarehouseCommand toCommand(CreateWarehouseRequest req) {
        return new CreateWarehouseCommand(req.city(), req.lat(), req.lng());
    }
    
    public static WarehouseResponse toResponse(Warehouse warehouse) {
        return new WarehouseResponse(
            warehouse.getWarehouseId(),
            warehouse.getCity(),
            warehouse.getLat(),
            warehouse.getLng()
        );
    }
}
