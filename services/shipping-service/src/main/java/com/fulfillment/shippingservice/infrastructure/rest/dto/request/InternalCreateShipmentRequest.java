package com.fulfillment.shippingservice.infrastructure.rest.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request interno para crear un envío")
public record InternalCreateShipmentRequest(
    @Schema(description = "ID de la orden", example = "8d91c9aa-1234-4567-890a-abcdef123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank String orderId,
    @Schema(description = "ID de la bodega", example = "warehouse-001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank String warehouseId,
    @Schema(description = "Ítems del envío", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty @Valid List<Item> items) {

    public record Item(
        @Schema(description = "SKU del producto", example = "SKU-1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String sku,
        @Schema(description = "Cantidad", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        @Positive int quantity) {
    }
}
