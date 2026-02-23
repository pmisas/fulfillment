package com.fulfillment.orderservice.infrastructure.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fulfillment.orderservice.domain.exception.IdempotencyInconsistentStateException;
import com.fulfillment.orderservice.domain.exception.InvalidStatusTransitionException;
import com.fulfillment.orderservice.domain.exception.OrderNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(OrderNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND) // devuelve 404
  public String handleOrderNotFound(OrderNotFoundException ex) {
      return ex.getMessage();
  }

  @ExceptionHandler(IdempotencyInconsistentStateException.class)
  @ResponseStatus(HttpStatus.CONFLICT) // devuelve 409
  public String handleIdempotency(IdempotencyInconsistentStateException ex) {
      return ex.getMessage();
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST) // 400
  public String handleDtoValidation(MethodArgumentNotValidException ex) {
    return "Validation failed";
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST) //400
  public String handleIllegalArg(IllegalArgumentException ex) {
    return ex.getMessage();
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) //500
  public String handleGeneric(Exception ex) {
    return "Unexpected error" + ex;
  }

  @ExceptionHandler(InvalidStatusTransitionException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST) // 400
  public String handleInvalidStatusTransition(InvalidStatusTransitionException ex) {
      return ex.getMessage();
  }

}
