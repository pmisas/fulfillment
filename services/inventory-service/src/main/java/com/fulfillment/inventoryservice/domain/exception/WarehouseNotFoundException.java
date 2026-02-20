package com.fulfillment.inventoryservice.domain.exception;

public class WarehouseNotFoundException extends RuntimeException{
    public WarehouseNotFoundException(String warehouseId) {
        super("warehouse not found" + warehouseId);
    }
}
