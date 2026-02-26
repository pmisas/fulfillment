package com.fulfillment.inventoryservice.application.dto;

import static com.fulfillment.inventoryservice.domain.shared.DomainValidations.requireNonBlank;

public record SkuQuantity(String sku, int quantity) {
    public SkuQuantity {
        sku = requireNonBlank(sku, "sku").trim();
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
    }
}
