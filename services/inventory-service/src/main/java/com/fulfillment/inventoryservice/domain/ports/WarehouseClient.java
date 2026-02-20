package com.fulfillment.inventoryservice.domain.ports;

public interface WarehouseClient {
    boolean existsById(String warehouseId);
}
