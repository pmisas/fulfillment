package com.fulfillment.orderservice.infrastructure.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fulfillment.orderservice.domain.exception.IdempotencyInconsistentStateException;
import com.fulfillment.orderservice.domain.exception.InvalidStatusTransitionException;
import com.fulfillment.orderservice.domain.exception.OrderNotFoundException;
import com.fulfillment.orderservice.infrastructure.rest.dto.ApiErrorResponse;
import com.fulfillment.orderservice.infrastructure.rest.dto.ApiErrorResponse.FieldViolation;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderNotFound(
            OrderNotFoundException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ApiErrorResponse body = new ApiErrorResponse(
                status.value(), "ORDER_NOT_FOUND", ex.getMessage(), null);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(IdempotencyInconsistentStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIdempotency(
            IdempotencyInconsistentStateException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.CONFLICT;
        ApiErrorResponse body = new ApiErrorResponse(
                status.value(), "IDEMPOTENCY_CONFLICT", ex.getMessage(), null);
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
