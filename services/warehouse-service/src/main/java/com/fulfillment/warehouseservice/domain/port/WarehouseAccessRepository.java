package com.fulfillment.warehouseservice.domain.port;

import java.util.List;
import java.util.Optional;

import com.fulfillment.warehouseservice.domain.model.WarehouseAccess;

public interface WarehouseAccessRepository {

    WarehouseAccess save(WarehouseAccess access);
    Optional<WarehouseAccess> findByUserId(String userId);
    List<WarehouseAccess> findActiveByWarehouseId(String warehouseId);
}
