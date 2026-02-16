package com.fulfillment.warehouseservice.infrastructure.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fulfillment.warehouseservice.domain.exception.WarehouseAlreadyExistsException;
import com.fulfillment.warehouseservice.domain.exception.WarehouseNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WarehouseAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT) //devuelve 409
    public String handleAlreadyExists(WarehouseAlreadyExistsException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(WarehouseNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND) //devuelve 404
    public String handleNotFound(WarehouseNotFoundException ex) {
        return ex.getMessage();
    }
}
