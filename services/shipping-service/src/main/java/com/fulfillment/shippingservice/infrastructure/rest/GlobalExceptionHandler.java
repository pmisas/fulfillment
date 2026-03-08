package com.fulfillment.shippingservice.infrastructure.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fulfillment.shippingservice.domain.exception.InvalidStatusTransitionException;
import com.fulfillment.shippingservice.domain.exception.ShipmentGuideNotReadyException;
import com.fulfillment.shippingservice.domain.exception.ShipmentNotFoundException;
import com.fulfillment.shippingservice.infrastructure.rest.dto.ApiErrorResponse;
import com.fulfillment.shippingservice.infrastructure.rest.dto.ApiErrorResponse.FieldViolation;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ShipmentGuideNotReadyException.class)
    public ResponseEntity<ApiErrorResponse> handleGuideNotReady(
            ShipmentGuideNotReadyException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.CONFLICT;
        ApiErrorResponse body = new ApiErrorResponse(
                status.value(), "GUIDE_NOT_READY", ex.getMessage(), null);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(ShipmentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleShipmentNotFound(
            ShipmentNotFoundException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ApiErrorResponse body = new ApiErrorResponse(
                status.value(), "SHIPMENT_NOT_FOUND", ex.getMessage(), null);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidStatusTransition(
            InvalidStatusTransitionException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiErrorResponse body = new ApiErrorResponse(
                status.value(), "INVALID_STATUS_TRANSITION", ex.getMessage(), null);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleDtoValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        List<FieldViolation> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new FieldViolation(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();

        ApiErrorResponse body = new ApiErrorResponse(
                status.value(), "VALIDATION_ERROR", "El request tiene campos invalidos.", fields);
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
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ApiErrorResponse body = new ApiErrorResponse(
                status.value(), "INTERNAL_ERROR", "Ha ocurrido un error inesperado.", null);
        return ResponseEntity.status(status).body(body);
    }
}
