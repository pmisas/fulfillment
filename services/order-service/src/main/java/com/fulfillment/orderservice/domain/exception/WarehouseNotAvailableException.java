package com.fulfillment.orderservice.domain.exception;

public class WarehouseNotAvailableException extends RuntimeException{
    
    public WarehouseNotAvailableException() {
        super("No warehouses available");
    }
}
