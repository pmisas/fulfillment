package com.fulfillment.orderservice.infrastructure.rest.dto;

import java.util.List;

public record ApiErrorResponse(
    int status,
    String error,
    String message,
    List<FieldViolation> fields
) {
    public record FieldViolation(String field, String message) {}
} 