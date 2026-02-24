package com.fulfillment.orderstateprocesor.infrastructure.client.dto;


public record WarehouseListResponse(
    String warehouseId, 
    String city,
    Double lat,
    Double lng) 
    
{}
