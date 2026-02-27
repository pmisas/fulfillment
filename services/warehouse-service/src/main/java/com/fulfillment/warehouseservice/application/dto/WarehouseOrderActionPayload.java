package com.fulfillment.warehouseservice.application.dto;

import static com.fulfillment.warehouseservice.domain.shared.DomainValidations.requireNonBlank;

public record WarehouseOrderActionPayload(
    String orderId,
    String warehouseId
) {
    public WarehouseOrderActionPayload {
        orderId = requireNonBlank(orderId, "orderId").trim();
        warehouseId = requireNonBlank(warehouseId, "warehouseId").trim();
    }
}
