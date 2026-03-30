package com.fulfillment.warehouseservice.infrastructure.rest.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de error de la API")
public record ApiErrorResponse(
    @Schema(description = "Código de estado HTTP", example = "404")
    int status,
    @Schema(description = "Código de error de la aplicación", example = "WAREHOUSE_NOT_FOUND")
    String error,
    @Schema(description = "Mensaje descriptivo del error", example = "Warehouse not found")
    String message,
    @Schema(description = "Violaciones de validación por campo")
    List<FieldViolation> fields
) {
    public record FieldViolation(
        @Schema(description = "Nombre del campo", example = "city")
        String field,
        @Schema(description = "Mensaje de la violación", example = "must not be blank")
        String message) {}
}
