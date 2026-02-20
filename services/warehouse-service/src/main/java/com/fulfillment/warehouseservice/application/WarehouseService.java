package com.fulfillment.warehouseservice.application;

import java.util.List;

import com.fulfillment.warehouseservice.application.dto.CreateWarehouseCommand;
import com.fulfillment.warehouseservice.domain.model.Warehouse;

public interface WarehouseService {

    Warehouse create(CreateWarehouseCommand command);
    Warehouse getById(String warehouseId);
    List<Warehouse> getAll();
    boolean existsAny();
    boolean existsById(String warehouseId);
}
