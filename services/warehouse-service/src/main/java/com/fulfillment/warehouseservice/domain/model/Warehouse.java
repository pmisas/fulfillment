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
                    String city) {
        Instant now = Instant.now();
        return new Warehouse(
            UUID.randomUUID().toString(), 
            city, 
            now);
    }

    public static Warehouse restore(
            String warehouseId,
            String city,
            Instant createdAt
    ) {
        return new Warehouse(warehouseId, city, createdAt);
    }

    public String getWarehouseId() {
        return this.warehouseId;
    }

    public String getCity() {
        return this.city;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }
}
