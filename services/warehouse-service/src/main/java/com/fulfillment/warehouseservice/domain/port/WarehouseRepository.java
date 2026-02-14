package com.fulfillment.warehouseservice.domain.port;

import java.util.List;
import java.util.Optional;

import com.fulfillment.warehouseservice.domain.model.Warehouse;

public interface WarehouseRepository {

    Warehouse save(Warehouse warehouse);
    Optional<Warehouse> findById(String warehouseId);
    List<Warehouse> findAll();
    Optional<Warehouse> findByCity(String city);
}
