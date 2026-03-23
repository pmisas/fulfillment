package com.fulfillment.inventoryservice.domain.exception;

public class WarehouseAccessDeniedException extends RuntimeException {

    public WarehouseAccessDeniedException(String userId, String warehouseId) {
        super("User " + userId + " cannot access warehouse " + warehouseId);
    }
}
