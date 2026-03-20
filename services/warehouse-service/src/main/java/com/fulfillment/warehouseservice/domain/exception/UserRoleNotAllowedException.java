package com.fulfillment.warehouseservice.domain.exception;

public class UserRoleNotAllowedException extends RuntimeException {

    public UserRoleNotAllowedException(String userId, String requiredRole) {
        super("User " + userId + " does not have required role " + requiredRole);
    }
}
