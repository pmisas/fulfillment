package com.fulfillment.warehouseservice.application;

import java.util.List;

import com.fulfillment.warehouseservice.domain.model.Warehouse;

public interface WarehouseService {

    Warehouse create(Warehouse warehouse);
    Warehouse getById(String id);
    List<Warehouse> getAll();
}
