package com.fulfillment.warehouseservice.infrastructure.drivenadapters.warehouserepository;

import java.time.Instant;

public class WarehouseEntity {

    private String warehouseId;
    private String city;
    private double lat;
    private double lng;
    private Instant createdAt;

    public WarehouseEntity(String warehouseId, String city, double lat, double lng, Instant createdAt) {
        this.warehouseId = warehouseId;
        this.city = city;
        this.lat = lat;
        this.lng = lng;
        this.createdAt = createdAt;
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
}
