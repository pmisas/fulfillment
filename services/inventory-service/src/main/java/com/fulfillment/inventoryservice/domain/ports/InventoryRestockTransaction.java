package com.fulfillment.inventoryservice.domain.ports;

import java.util.List;

public interface InventoryRestockTransaction {
    
    void restockAtomically(String warehouseId, List<Item> items);

    record Item(String sku, int quantity) {}
}
