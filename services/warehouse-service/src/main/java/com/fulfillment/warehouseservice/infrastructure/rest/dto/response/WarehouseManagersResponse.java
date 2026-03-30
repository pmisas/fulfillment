package com.fulfillment.warehouseservice.infrastructure.rest.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Managers activos asignados a una bodega")
public record WarehouseManagersResponse(
    @Schema(description = "Identificador de la bodega", example = "warehouse-001")
    String warehouseId,
    @Schema(description = "Lista de userId activos asignados")
    List<String> managerUserIds
) {}
