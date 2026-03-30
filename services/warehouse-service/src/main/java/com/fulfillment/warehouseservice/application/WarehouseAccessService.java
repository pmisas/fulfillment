package com.fulfillment.warehouseservice.application;

import java.util.List;

import com.fulfillment.warehouseservice.domain.model.WarehouseAccess;

public interface WarehouseAccessService {

    WarehouseAccess assignManager(String warehouseId, String userId, String assignedBy);
    WarehouseAccess removeManager(String warehouseId, String userId);
    List<String> getManagersByWarehouse(String warehouseId);
    WarehouseAccess getWarehouseAccessByUser(String userId);
}
