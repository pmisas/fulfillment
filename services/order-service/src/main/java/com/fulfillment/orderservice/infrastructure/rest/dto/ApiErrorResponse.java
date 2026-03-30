package com.fulfillment.orderservice.infrastructure.rest.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de error para la API, incluyendo detalles de validación de campos")
public record ApiErrorResponse(
    @Schema(description = "Código de estado HTTP de la respuesta", example = "400")
    int status,

    @Schema(description = "Código de error específico de la aplicación", example = "VALIDATION_ERROR")
    String error,

    @Schema(description = "Mensaje descriptivo del error", example = "Validation failed for one or more fields.")
    String message,

    @Schema(description = "Lista de violaciones de campos, si el error está relacionado con validación de entrada")
    List<FieldViolation> fields
) {
    public record FieldViolation(
        @Schema(description = "Nombre del campo que violó la validación", example = "lat")
        String field,

        @Schema(description = "Mensaje de la violación de validación", example = "Latitude must be between -90 and 90")
        String message
    ) {}
} 