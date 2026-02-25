package com.fulfillment.inventoryservice.application;

import java.util.List;

import com.fulfillment.inventoryservice.application.dto.AvailabilityQuery;
import com.fulfillment.inventoryservice.application.dto.AvailabilityResult;
import com.fulfillment.inventoryservice.application.dto.InventoryCommand;
import com.fulfillment.inventoryservice.domain.model.InventoryItem;

public interface InventoryItemsService {
    
    InventoryItem consume(InventoryCommand command);
    InventoryItem restock(InventoryCommand command);
    InventoryItem reserve(InventoryCommand command);
    InventoryItem release(InventoryCommand command);
    List<InventoryItem> lowStock(int min);
    List<InventoryItem> getByWarehouseId(String warehouseId);
    AvailabilityResult checkAvailability(AvailabilityQuery query);

}
