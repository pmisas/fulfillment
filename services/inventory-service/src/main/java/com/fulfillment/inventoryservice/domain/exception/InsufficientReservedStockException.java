package com.fulfillment.inventoryservice.domain.exception;

public class InsufficientReservedStockException extends RuntimeException{
    public InsufficientReservedStockException(int amount, String warehouseId, String sku, int reserved) {
        super("Insufficient reserve, warehouseId= "+ warehouseId+ 
                        " sku= " + sku + 
                        " amount= " + amount + 
                        " reserved " + reserved);
    }
}
