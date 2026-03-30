package com.fulfillment.warehouseservice.infrastructure.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos de una bodega")
public record WarehouseResponse(
    @Schema(description = "ID de la bodega", example = "warehouse-001")
    String warehouseId,
    @Schema(description = "Ciudad donde está ubicada la bodega", example = "Bogotá")
    String city,
    @Schema(description = "Latitud de la bodega", example = "4.7110")
    Double lat,
    @Schema(description = "Longitud de la bodega", example = "-74.0721")
    Double lng
) {}

