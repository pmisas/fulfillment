package com.fulfillment.shippingservice.domain.model;

import lombok.Getter;

@Getter
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

    public boolean isActive() {
        return active;
    }
}
