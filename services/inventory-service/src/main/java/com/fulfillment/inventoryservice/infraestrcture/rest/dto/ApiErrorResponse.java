package com.fulfillment.inventoryservice.infraestrcture.rest.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de error de la API")
public record ApiErrorResponse(
    @Schema(description = "Código de estado HTTP", example = "400")
    int status,
    @Schema(description = "Código de error de la aplicación", example = "VALIDATION_ERROR")
    String error,
    @Schema(description = "Mensaje descriptivo del error", example = "El request tiene campos inválidos.")
    String message,
    @Schema(description = "Violaciones de validación por campo")
    List<FieldViolation> fields
) {
    public record FieldViolation(
        @Schema(description = "Nombre del campo", example = "items")
        String field,
        @Schema(description = "Mensaje de la violación", example = "must not be empty")
        String message) {}
} 
