package com.fulfillment.warehouseservice.domain.exception;

public class WarehouseAccessNotFoundException extends RuntimeException {

    public WarehouseAccessNotFoundException(String userId) {
        super("Warehouse access not found for user: " + userId);
    }
}
