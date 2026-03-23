package com.fulfillment.inventoryservice.domain.ports;

import java.util.Optional;

import com.fulfillment.inventoryservice.domain.model.WarehouseAccess;

public interface WarehouseAccessRepository {

    Optional<WarehouseAccess> findByUserId(String userId);
}
