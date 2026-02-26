package com.fulfillment.inventoryservice.infraestrcture.rest;

import java.util.List;

import com.fulfillment.inventoryservice.application.dto.AvailabilityQuery;
import com.fulfillment.inventoryservice.application.dto.AvailabilityResult;
import com.fulfillment.inventoryservice.application.dto.ReserveBatchCommand;
import com.fulfillment.inventoryservice.application.dto.RestockBatchCommand;
import com.fulfillment.inventoryservice.application.dto.SkuQuantity;
import com.fulfillment.inventoryservice.domain.model.InventoryItem;
import com.fulfillment.inventoryservice.infraestrcture.rest.dto.BatchRequest;
import com.fulfillment.inventoryservice.infraestrcture.rest.dto.CheckAvailabilityResponse;
import com.fulfillment.inventoryservice.infraestrcture.rest.dto.InventoryItemResponse;
import com.fulfillment.inventoryservice.infraestrcture.rest.dto.ReserveItemsRequest;

public class InventoryRestMapper {

    private InventoryRestMapper() {}

    public static RestockBatchCommand toRestockBatchCommand(String warehouseId, BatchRequest req) {
        return new RestockBatchCommand(
                warehouseId,
                req.items().stream()
                        .map(i -> new SkuQuantity(i.sku(), i.quantity()))
                        .toList()
        );
    }

    public static ReserveBatchCommand toReserveBatchCommand(String warehouseId, ReserveItemsRequest req) {
        return new ReserveBatchCommand(
                req.reservationId(),
                req.orderId(),
                warehouseId,
                req.items().stream()
                        .map(i -> new SkuQuantity(i.sku(), i.quantity()))
                        .toList()
        );
    }

    public static AvailabilityQuery toAvailabilityQuery(String warehouseId, BatchRequest req) {
        List<SkuQuantity> items = req.items().stream()
                .map(i -> new SkuQuantity(i.sku(), i.quantity()))
                .toList();
        return new AvailabilityQuery(warehouseId, items);
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
