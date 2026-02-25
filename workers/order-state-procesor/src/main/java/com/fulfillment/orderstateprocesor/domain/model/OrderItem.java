package com.fulfillment.orderstateprocesor.domain.model;

import static com.fulfillment.orderstateprocesor.domain.shared.DomainValidations.requireNonBlank;

import lombok.Getter;

@Getter
public class OrderItem {
    private final String sku;
    private final int quantity;

    private OrderItem(String sku, int quantity) {
        this.sku = requireNonBlank(sku, "sku").trim();
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        this.quantity = quantity;
    }

    public static OrderItem create(String sku, int quantity) {
        return new OrderItem(sku, quantity);
    }

} 
