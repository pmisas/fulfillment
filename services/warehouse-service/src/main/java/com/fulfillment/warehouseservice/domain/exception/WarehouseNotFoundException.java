package com.fulfillment.warehouseservice.domain.exception;

public class WarehouseNotFoundException extends RuntimeException{
    public WarehouseNotFoundException(String warehouseId) {
        super("Warehouse not found exception" + warehouseId);
    }
}
