package com.fulfillment.warehouseservice.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request para asignar un manager a una bodega")
public record AssignWarehouseManagerRequest(
    @NotBlank
    @Schema(description = "Identificador del usuario en Cognito (sub)", example = "6b5adfd0-3b58-4f95-bf4d-cc6e45b94d60")
    String userId
) {}
