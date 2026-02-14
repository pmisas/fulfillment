package com.fulfillment.warehouseservice.domain.exception;

public class WarehouseAlreadyExistsException extends RuntimeException{

    public WarehouseAlreadyExistsException(String city) {
        super("Warehouse in city '" + city + "' already exists");
    }
}