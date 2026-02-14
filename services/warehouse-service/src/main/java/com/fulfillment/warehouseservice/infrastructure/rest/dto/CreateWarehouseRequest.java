package com.fulfillment.warehouseservice.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateWarehouseRequest {

    @NotBlank
    private String city;


    public String getCity() {
        return city;
    }

}
