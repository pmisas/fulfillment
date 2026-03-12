package com.fulfillment.orderservice.infrastructure.rest.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request para crear una orden")
public record CreateOrderRequest(

    @Schema(description = "Items de la orden", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty @Valid List<Item> items,

    @Schema(description = "Latitud del destino de la orden", requiredMode = Schema.RequiredMode.REQUIRED, example = "-34.6037")
    @NotNull Double lat,
    
    @Schema(description = "Longitud del destino de la orden", requiredMode = Schema.RequiredMode.REQUIRED, example = "-58.3816")
    @NotNull Double lng

    ) {
    public record Item(
        @Schema(description = "SKU del producto", example = "SKU-1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String sku,
        
        @Schema(description = "Cantidad solicitada", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        @Positive int quantity
    ) {}
}
