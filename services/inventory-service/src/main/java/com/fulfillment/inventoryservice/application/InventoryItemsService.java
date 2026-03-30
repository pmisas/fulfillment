package com.fulfillment.inventoryservice.application;

import java.util.List;

import com.fulfillment.inventoryservice.application.dto.AvailabilityQuery;
import com.fulfillment.inventoryservice.application.dto.AvailabilityResult;
import com.fulfillment.inventoryservice.application.dto.SkuQuantity;
import com.fulfillment.inventoryservice.domain.model.InventoryItem;
import com.fulfillment.inventoryservice.domain.ports.InventoryReservationTransaction.ReserveResult;
import com.fulfillment.inventoryservice.domain.ports.InventoryReservationTransaction.ConsumeResult;

public interface InventoryItemsService {
    
    ConsumeResult consumeReservation(String reservationId);
    List<InventoryItem> restockBatch(String warehouseId, List<SkuQuantity> items);

    List<InventoryItem> lowStock(int min);
    List<InventoryItem> getByWarehouseId(String warehouseId);
    AvailabilityResult checkAvailability(AvailabilityQuery query);

    ReserveResult reserveItems(String reservationId, String orderId, String warehouseId, List<SkuQuantity> items);
    void releaseReservation(String reservationId);

}
