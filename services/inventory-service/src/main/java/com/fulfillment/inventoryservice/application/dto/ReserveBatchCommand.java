package com.fulfillment.inventoryservice.application.dto;

import java.util.List;

public record ReserveBatchCommand(
  String reservationId,
  String orderId,
  String warehouseId,
  List<SkuQuantity> items
) {}
