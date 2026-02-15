package com.fulfillment.warehouseservice.infrastructure.rest.dto;

public class WarehouseResponse {
    
    private final String warehouseId;
    private final String city;

    public WarehouseResponse(String warehouseId, String city) {
        this.warehouseId = warehouseId;
        this.city = city;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getCity() {
        return city;
    }
    
}
