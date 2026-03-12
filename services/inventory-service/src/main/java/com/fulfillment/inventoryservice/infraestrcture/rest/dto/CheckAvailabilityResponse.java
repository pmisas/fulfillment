package com.fulfillment.inventoryservice.infraestrcture.rest.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de verificación de disponibilidad de inventario")
public record CheckAvailabilityResponse(
    @Schema(description = "Indica si todos los ítems pueden ser satisfechos", example = "true")
    boolean canFulfillAll,
    @Schema(description = "Detalle de disponibilidad por ítem")
    List<ItemAvailability> items
) {
    public record ItemAvailability(
        @Schema(description = "SKU del producto", example = "SKU-1")
        String sku,
        @Schema(description = "Cantidad requerida", example = "5")
        int required,
        @Schema(description = "Cantidad disponible en inventario", example = "10")
        int available,
        @Schema(description = "Si la cantidad requerida puede ser satisfecha", example = "true")
        boolean canFulfill
    ) {}
}
