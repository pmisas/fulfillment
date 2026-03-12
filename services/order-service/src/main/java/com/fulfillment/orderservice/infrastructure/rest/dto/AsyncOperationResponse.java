package com.fulfillment.orderservice.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta para operaciones asíncronas como cancelación de orden")
public record AsyncOperationResponse(
    @Schema(description = "ID de la orden afectada por la operación asíncrona", example = "8d91c9aa-1234-4567-890a-abcdef123456")
    String orderId,

    @Schema(description = "Mensaje descriptivo del estado de la operación asíncrona", example = "Order cancellation has been requested and is being processed. The order will be cancelled shortly and inventory will be released.")
    String message,

    @Schema(description = "Estado actual de la operación asíncrona", example = "PROCESSING")
    String status
) {
    public static AsyncOperationResponse cancellationRequested(String orderId) {
        return new AsyncOperationResponse(
            orderId,
            "Order cancellation has been requested and is being processed. The order will be cancelled shortly and inventory will be released.",
            "PROCESSING"
        );
    }
}
