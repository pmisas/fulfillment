package com.fulfillment.inventoryservice.application.dto;

public record InventoryCommand (
    String warehouseId,
    String sku,
    int amount
) {}
