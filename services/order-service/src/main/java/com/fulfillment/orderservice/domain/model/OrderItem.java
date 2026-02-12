package com.fulfillment.orderservice.domain.model;

import static com.fulfillment.orderservice.domain.shared.DomainValidations.requireNonBlank;

public class OrderItem {
    
    private final String sku;
    private final int quantity;

    private OrderItem (String sku, int quantity) {
        this.sku = requireNonBlank(sku, "sku");
        if (quantity <= 0) throw new
            IllegalArgumentException("quantity must be > 0");
        this.quantity = quantity;
    }

    public OrderItem createOrderItem(String sku, int quantity) {
        return new OrderItem(sku, quantity);
    }

    public String getSku() {
        return this.sku;
    }

    public int getQuantity() {
        return this.quantity;
    }

}
