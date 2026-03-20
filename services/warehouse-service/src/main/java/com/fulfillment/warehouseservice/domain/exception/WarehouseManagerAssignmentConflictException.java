package com.fulfillment.warehouseservice.domain.exception;

public class WarehouseManagerAssignmentConflictException extends RuntimeException {

    public WarehouseManagerAssignmentConflictException(String userId, String warehouseId) {
        super("User " + userId + " already has an active assignment for warehouse " + warehouseId);
    }
}
