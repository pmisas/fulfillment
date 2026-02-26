package com.fulfillment.warehouseservice.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import lombok.Getter;

import static com.fulfillment.warehouseservice.domain.shared.DomainValidations.requireNonBlank;

@Getter
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
        this.city = requireNonBlank(city.trim().toLowerCase(), "city");

        if (lat < -90 || lat > 90) 
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        
        if (lng < -180 || lng > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }

        this.lat = lat;
        this.lng = lng;
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

}
