package com.fulfillment.inventoryservice.infraestrcture.rest;

import com.fulfillment.inventoryservice.application.dto.InventoryCommand;
import com.fulfillment.inventoryservice.domain.model.InventoryItem;
import com.fulfillment.inventoryservice.infraestrcture.rest.dto.InventoryItemResponse;

public class InventoryRestMapper {
    
    private InventoryRestMapper() {}

    public static InventoryCommand toCommand(
        String warehouseId,
        String sku,
        int amount
    ) {
        return new InventoryCommand(warehouseId, sku, amount);
    }

    public static InventoryItemResponse toResponse(InventoryItem item) {
        return new InventoryItemResponse(
            item.getWarehouseId(), 
            item.getSku(),
            item.getQuantity(), 
            item.getUpdatedAt()
        );
    }
}
