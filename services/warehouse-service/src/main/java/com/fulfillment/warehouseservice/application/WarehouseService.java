package com.fulfillment.warehouseservice.application;

import java.util.List;

import com.fulfillment.warehouseservice.domain.model.Warehouse;

public interface WarehouseService {

    Warehouse create(String city, double lat, double lng);
    Warehouse getById(String warehouseId);
    List<Warehouse> getAll();
    boolean existsById(String warehouseId);

    void completePicking(String warehouseId, String orderId);
    void completePacking(String warehouseId, String orderId);
}
