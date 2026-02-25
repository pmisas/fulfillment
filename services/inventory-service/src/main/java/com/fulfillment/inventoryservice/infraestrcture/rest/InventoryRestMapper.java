package com.fulfillment.inventoryservice.infraestrcture.rest;

import java.util.List;

import com.fulfillment.inventoryservice.application.dto.AvailabilityResult;
import com.fulfillment.inventoryservice.application.dto.InventoryCommand;
import com.fulfillment.inventoryservice.domain.model.InventoryItem;
import com.fulfillment.inventoryservice.infraestrcture.rest.dto.CheckAvailabilityResponse;
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
            item.getReserved(),
            item.available(),
            item.getUpdatedAt()
        );
    }

    public static CheckAvailabilityResponse toAvailabilityResponse(AvailabilityResult result) {
        List<CheckAvailabilityResponse.ItemAvailability> items = result.items().stream()
            .map(i -> new CheckAvailabilityResponse.ItemAvailability(
                i.sku(), i.required(), i.available(), i.canFulfill()))
            .toList();
        return new CheckAvailabilityResponse(result.canFulfillAll(), items);
    }
}
