package com.fulfillment.inventoryservice.application.dto;

import java.util.List;

public record RestockBatchCommand(String warehouseId, List<SkuQuantity> items) {}
