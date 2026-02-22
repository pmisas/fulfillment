package com.fulfillment.orderstateprocesor.domain.shared;

public class DomainValidations {
    public static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
