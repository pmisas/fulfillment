package com.fulfillment.warehouseservice.application.dto;

public record AssignWarehouseManagerCommand(
    String warehouseId,
    String userId,
    String assignedBy
) {}
