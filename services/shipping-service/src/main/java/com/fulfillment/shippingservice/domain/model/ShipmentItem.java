package com.fulfillment.shippingservice.domain.model;

import static com.fulfillment.shippingservice.domain.shared.DomainValidations.requireNonBlank;

public class ShipmentItem {

    private final String sku;
    private final int quantity;

    private ShipmentItem(String sku, int quantity) {
        this.sku = requireNonBlank(sku, "sku");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
        this.quantity = quantity;
    }

    public static ShipmentItem createShipmentItem(String sku, int quantity) {
        return new ShipmentItem(sku, quantity);
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }
}
