package com.fulfillment.inventoryservice.infraestrcture.rest.dto.response;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ítem de inventario en una bodega")
public record InventoryItemResponse(
    @Schema(description = "ID de la bodega", example = "warehouse-001")
    String warehouseId,
    @Schema(description = "SKU del producto", example = "SKU-1")
    String sku,
    @Schema(description = "Cantidad total en stock", example = "100")
    int quantity,
    @Schema(description = "Cantidad reservada", example = "10")
    int reserved,
    @Schema(description = "Cantidad disponible para nuevas órdenes", example = "90")
    int available,
    @Schema(description = "Timestamp de la última actualización")
    Instant updateAt
) {}
