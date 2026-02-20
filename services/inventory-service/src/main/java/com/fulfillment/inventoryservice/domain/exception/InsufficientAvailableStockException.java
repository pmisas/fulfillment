package com.fulfillment.inventoryservice.domain.exception;

public class InsufficientAvailableStockException extends RuntimeException {
    public InsufficientAvailableStockException(int amount, String warehouseId, String sku, int available) {
        super("Insufficient stock to reserve. amount= " + amount + 
                    "warehouse= "+ warehouseId + 
                    "sku= "+ sku + 
                    "available="+ available);
    }
}
