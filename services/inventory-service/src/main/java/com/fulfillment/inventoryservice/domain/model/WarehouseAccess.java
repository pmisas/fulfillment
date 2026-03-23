package com.fulfillment.inventoryservice.domain.model;

public class WarehouseAccess {

    private final String userId;
    private final String warehouseId;
    private final boolean active;

    private WarehouseAccess(String userId, String warehouseId, boolean active) {
        this.userId = userId;
        this.warehouseId = warehouseId;
        this.active = active;
    }

    public static WarehouseAccess restore(String userId, String warehouseId, boolean active) {
        return new WarehouseAccess(userId, warehouseId, active);
    }

    public String getUserId() {
        return userId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public boolean isActive() {
        return active;
    }
}
