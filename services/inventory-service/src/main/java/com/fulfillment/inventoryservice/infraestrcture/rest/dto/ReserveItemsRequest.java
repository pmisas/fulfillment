package com.fulfillment.inventoryservice.infraestrcture.rest.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@Schema(description = "Request para reservar ítems de inventario")
public record ReserveItemsRequest(
    @Schema(description = "ID único de la reserva", example = "res-abc123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank String reservationId,
    @Schema(description = "ID de la orden asociada", example = "8d91c9aa-1234-4567-890a-abcdef123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank String orderId,
    @Schema(description = "Ítems a reservar", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty @Valid List<SkuQuantity> items) {
    public record SkuQuantity(
        @Schema(description = "SKU del producto", example = "SKU-1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String sku,
        @Schema(description = "Cantidad a reservar", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(1) int quantity
    ) {}
}
