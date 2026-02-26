package com.fulfillment.inventoryservice.application;

import java.util.List;

import com.fulfillment.inventoryservice.application.dto.AvailabilityQuery;
import com.fulfillment.inventoryservice.application.dto.AvailabilityResult;
import com.fulfillment.inventoryservice.application.dto.ConsumeReservationCommand;
import com.fulfillment.inventoryservice.application.dto.ReserveBatchCommand;
import com.fulfillment.inventoryservice.application.dto.RestockBatchCommand;
import com.fulfillment.inventoryservice.domain.model.InventoryItem;
import com.fulfillment.inventoryservice.domain.ports.InventoryReservationTransaction.ReserveResult;
import com.fulfillment.inventoryservice.domain.ports.InventoryReservationTransaction.ConsumeResult;

public interface InventoryItemsService {
    
    ConsumeResult consumeReservation(ConsumeReservationCommand command);
    List<InventoryItem> restockBatch(RestockBatchCommand command);

    List<InventoryItem> lowStock(int min);
    List<InventoryItem> getByWarehouseId(String warehouseId);
    AvailabilityResult checkAvailability(AvailabilityQuery query);

    ReserveResult reserveItems(ReserveBatchCommand command);
    void releaseReservation(String reservationId);

}
