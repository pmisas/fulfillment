package com.fulfillment.inventoryservice.domain.ports;

import java.util.List;
import java.util.Optional;

import com.fulfillment.inventoryservice.domain.model.InventoryItem;

public interface InventoryItemsRepository {
    
    InventoryItem save(InventoryItem item);
    Optional<InventoryItem> findById(String warehouseId, String sku);
    List<InventoryItem> findByWarehouseId(String warehouseId);
    List<InventoryItem> findBySkus(String warehouseId, List<String> skus);
    List<InventoryItem> findLowStock(int min);
}
