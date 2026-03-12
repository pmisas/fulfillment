package com.fulfillment.orderservice.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta resumida de una orden")
public record OrderResponse(
    
    @Schema(description = "ID de la orden", example = "8d91c9aa-1234-4567-890a-abcdef123456")
    String orderId,
    
    @Schema(description = "Estado de la orden", example = "CREATED")
    String status
) {
}
