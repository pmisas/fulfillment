package com.fulfillment.inventoryservice.infraestrcture.rest.dto;

import java.time.Instant;

public record InventoryItemResponse(
    String warehouseId,
    String sku,
    int quantity,
    int reserved,
    int available,
    Instant updateAt
) {}
