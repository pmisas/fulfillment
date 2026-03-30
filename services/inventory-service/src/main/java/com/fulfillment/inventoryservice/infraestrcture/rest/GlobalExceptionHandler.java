package com.fulfillment.inventoryservice.infraestrcture.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fulfillment.inventoryservice.domain.exception.InsufficientAvailableStockException;
import com.fulfillment.inventoryservice.domain.exception.InsufficientReservedStockException;
import com.fulfillment.inventoryservice.domain.exception.WarehouseAccessDeniedException;
import com.fulfillment.inventoryservice.domain.exception.WarehouseNotFoundException;
import com.fulfillment.inventoryservice.infraestrcture.rest.dto.response.ApiErrorResponse;
import com.fulfillment.inventoryservice.infraestrcture.rest.dto.response.ApiErrorResponse.FieldViolation;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InsufficientAvailableStockException.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientAvailableStock(
            InsufficientAvailableStockException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
        ApiErrorResponse body = new ApiErrorResponse(
                status.value(), "INSUFFICIENT_AVAILABLE_STOCK", ex.getMessage(), null);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(InsufficientReservedStockException.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientReservedStock(
            InsufficientReservedStockException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
        ApiErrorResponse body = new ApiErrorResponse(
                status.value(), "INSUFFICIENT_RESERVED_STOCK", ex.getMessage(), null);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(WarehouseNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleWarehouseNotFound(
            WarehouseNotFoundException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ApiErrorResponse body = new ApiErrorResponse(
                status.value(), "WAREHOUSE_NOT_FOUND", ex.getMessage(), null);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler({WarehouseAccessDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<ApiErrorResponse> handleForbidden(
            Exception ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        ApiErrorResponse body = new ApiErrorResponse(
                status.value(), "FORBIDDEN", ex.getMessage(), null);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleDtoValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        List<FieldViolation> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldViolation(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ApiErrorResponse body = new ApiErrorResponse(
                status.value(), "VALIDATION_ERROR", "El request tiene campos inválidos.", fields);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArg(
            IllegalArgumentException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiErrorResponse body = new ApiErrorResponse(
                status.value(), "BAD_REQUEST", ex.getMessage(), null);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ApiErrorResponse body = new ApiErrorResponse(
                status.value(), "INTERNAL_ERROR", ex.getMessage(), null);
        return ResponseEntity.status(status).body(body);
    }
}
