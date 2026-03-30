package com.fulfillment.warehouseservice.infrastructure.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request para crear una bodega")
public record CreateWarehouseRequest(
    @Schema(description = "Ciudad donde está ubicada la bodega", requiredMode = Schema.RequiredMode.REQUIRED, example = "Bogotá")
    @NotBlank String city,
    @Schema(description = "Latitud de la bodega", requiredMode = Schema.RequiredMode.REQUIRED, example = "4.7110")
    @NotNull Double lat,
    @Schema(description = "Longitud de la bodega", requiredMode = Schema.RequiredMode.REQUIRED, example = "-74.0721")
    @NotNull Double lng
) {}

