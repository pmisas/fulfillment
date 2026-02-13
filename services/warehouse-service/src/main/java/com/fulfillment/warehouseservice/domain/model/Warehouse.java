package com.fulfillment.warehouseservice.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static com.fulfillment.warehouseservice.domain.shared.DomainValidations.requireNonBlank;

public class Warehouse {
    private final String warehouseId;
    private final String city;
    private final Instant createdAt;

    private Warehouse(
            String warehouseId,
            String city,
            Instant createdAt){
        this.warehouseId = requireNonBlank(warehouseId, "warehouseId");
        this.city = requireNonBlank(city, "city");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static Warehouse createWarehouse(
                    String city,
                    Instant createdAt) {
        Instant now = Instant.now();
        return new Warehouse(
            UUID.randomUUID().toString(), 
            city, 
            now);
    }

    public String getWarehouseId() {
        return this.warehouseId;
    }

    public String getCity() {
        return this.city;
    }

    public Instant getreatedAt() {
        return this.createdAt;
    }
}
