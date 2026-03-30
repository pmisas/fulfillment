package com.fulfillment.inventoryservice.infraestrcture.rest.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@Schema(description = "Request con lista de SKUs y cantidades")
public record BatchRequest(
    @Schema(description = "Ítems del batch", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty @Valid List<SkuQuantity> items) {
    public record SkuQuantity(
        @Schema(description = "SKU del producto", example = "SKU-1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String sku,
        @Schema(description = "Cantidad", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(1) int quantity) {}
}
