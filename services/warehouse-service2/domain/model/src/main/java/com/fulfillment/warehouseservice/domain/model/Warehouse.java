package com.fulfillment.warehouseservice.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Warehouse {

    private final String warehouseId;
    private final String city;
    private final double lat;
    private final double lng;
    private final Instant createdAt;

    public Warehouse(String warehouseId, String city, double lat, double lng, Instant createdAt) {
        this.warehouseId = requireText(warehouseId, "warehouseId is required");
        this.city = requireText(city, "city is required");
        validateLatitude(lat);
        validateLongitude(lng);
        this.lat = lat;
        this.lng = lng;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
    }

    public static Warehouse create(String city, double lat, double lng) {
        return new Warehouse(UUID.randomUUID().toString(), city, lat, lng, Instant.now());
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getCity() {
        return city;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static void validateLatitude(double value) {
        if (value < -90 || value > 90) {
            throw new IllegalArgumentException("lat must be between -90 and 90");
        }
    }

    private static void validateLongitude(double value) {
        if (value < -180 || value > 180) {
            throw new IllegalArgumentException("lng must be between -180 and 180");
        }
    }
}
