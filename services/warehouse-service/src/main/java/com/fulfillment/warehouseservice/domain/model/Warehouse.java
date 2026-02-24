package com.fulfillment.warehouseservice.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static com.fulfillment.warehouseservice.domain.shared.DomainValidations.requireNonBlank;

public class Warehouse {
    private final String warehouseId;
    private final String city;
    private final double lat;
    private final double lng;
    private final Instant createdAt;

    private Warehouse(
            String warehouseId,
            String city,
            double lat,
            double lng,
            Instant createdAt){
        this.warehouseId = requireNonBlank(warehouseId, "warehouseId");
        this.city = requireNonBlank(city, "city");
        this.lat = 0;
        this.lng = 0;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static Warehouse createWarehouse(
                    String city,
                    double lat,
                    double lng) {
        Instant now = Instant.now();
        return new Warehouse(
            UUID.randomUUID().toString(), 
            city, 
            lat, lng, now);
    }

    public static Warehouse restore(
            String warehouseId,
            String city,
            double lat, 
            double lng,
            Instant createdAt
    ) {
        return new Warehouse(warehouseId, city, lat, lng , createdAt);
    }

    public String getWarehouseId() {
        return this.warehouseId;
    }

    public String getCity() {
        return this.city;
    }

    public double getLat() {
        return this.lat;
    }   
    
    public double getLng() {
        return this.lng;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

}
