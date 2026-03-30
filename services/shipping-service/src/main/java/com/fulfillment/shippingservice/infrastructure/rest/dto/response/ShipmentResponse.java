package com.fulfillment.shippingservice.infrastructure.rest.dto.response;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Detalle de un envío")
public record ShipmentResponse(
        @Schema(description = "ID del envío", example = "ship-abc123")
        String shipmentId,
        @Schema(description = "ID de la orden asociada", example = "8d91c9aa-1234-4567-890a-abcdef123456")
        String orderId,
        @Schema(description = "ID de la bodega de origen", example = "warehouse-001")
        String warehouseId,
        @Schema(description = "Transportista", example = "INTERNAL_CARRIER")
        String carrier,
        @Schema(description = "Estado del envío", example = "CREATED")
        String status,
        @Schema(description = "Número de guía de rastreo", example = "TRK-00123")
        String trackingId,
        @Schema(description = "Ítems incluidos en el envío")
        List<Item> items,
        @Schema(description = "Timestamp de creación del envío")
        Instant createdAt,
        @Schema(description = "Timestamp de despacho")
        Instant shippedAt,
        @Schema(description = "Fecha estimada de entrega")
        Instant estimatedDeliveryAt,
        @Schema(description = "Clave S3 de la guía de despacho", example = "guides/ship-abc123.pdf")
        String shippingGuideS3Key) {

    public record Item(
        @Schema(description = "SKU del producto", example = "SKU-1")
        String sku,
        @Schema(description = "Cantidad", example = "2")
        int quantity) {
    }
}
