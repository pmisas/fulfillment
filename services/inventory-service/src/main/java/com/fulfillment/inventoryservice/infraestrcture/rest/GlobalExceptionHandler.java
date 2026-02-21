package com.fulfillment.inventoryservice.infraestrcture.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fulfillment.inventoryservice.domain.exception.InsufficientAvailableStockException;
import com.fulfillment.inventoryservice.domain.exception.InsufficientReservedStockException;
import com.fulfillment.inventoryservice.domain.exception.WarehouseNotFoundException;


@RestControllerAdvice
public class GlobalExceptionHandler {
    
  @ExceptionHandler(InsufficientAvailableStockException.class)
  @ResponseStatus(HttpStatus.CONFLICT) // devuelve 409
  public String handleInsufficientAvailableStock(InsufficientAvailableStockException ex) {
      return ex.getMessage();
  }

  @ExceptionHandler(InsufficientReservedStockException.class)
  @ResponseStatus(HttpStatus.CONFLICT) // devuelve 409
  public String handleInsufficientReservedStock(InsufficientReservedStockException ex) {
      return ex.getMessage();
  }

  @ExceptionHandler(WarehouseNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND) // devuelve 404
  public String handleWarehouseNotFound(WarehouseNotFoundException ex) {
      return ex.getMessage();
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST) //400
  public String handleIllegalArg(IllegalArgumentException ex) {
    return ex.getMessage();
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) //500
  public String handleGeneric(Exception ex) {
    return "Unexpected error"+ ex;
  }

}
