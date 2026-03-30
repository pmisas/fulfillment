package com.fulfillment.warehouseservice.infrastructure.rest.dto.response;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Asignacion de acceso de un usuario a una bodega")
public record UserWarehouseAccessResponse(
    @Schema(description = "Identificador del usuario", example = "6b5adfd0-3b58-4f95-bf4d-cc6e45b94d60")
    String userId,
    @Schema(description = "Identificador de la bodega", example = "warehouse-001")
    String warehouseId,
    @Schema(description = "Indica si la asignacion esta activa", example = "true")
    boolean active,
    @Schema(description = "Fecha de asignacion")
    Instant assignedAt,
    @Schema(description = "Administrador que hizo la asignacion")
    String assignedBy,
    @Schema(description = "Fecha de la ultima actualizacion")
    Instant updatedAt
) {}
