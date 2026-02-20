package com.fulfillment.inventoryservice.domain.exception;

public class InsufficientAvailableException extends RuntimeException {
    public InsufficientAvailableException(int amount, String warehouseId, String sku, int available) {
        super("Insufficient stock to reserve. amount= " + amount + 
                    "warehouse= "+ warehouseId + 
                    "sku= "+ sku + 
                    "available="+ available);
    }
}
