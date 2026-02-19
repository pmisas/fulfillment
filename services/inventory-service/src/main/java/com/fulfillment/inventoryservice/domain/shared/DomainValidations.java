package com.fulfillment.inventoryservice.domain.shared;

public class DomainValidations {

    
    public static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must be not blank");
        }
        return value;
    }
}
