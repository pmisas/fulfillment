package com.fulfillment.inventoryservice.domain.exception;

public class InsufficientReservedException extends RuntimeException{
    public InsufficientReservedException(int amount, String warehouseId, String sku, int reserved) {
        super("Insufficient reserve, warehouseId= "+ warehouseId+ 
                        " sku= " + sku + 
                        " amount= " + amount + 
                        " reserved " + reserved);
    }
}
