package com.fulfillment.orderstateprocesor.infrastructure.client.warehouse.dto;

public record WarehouseListResponse(
    String warehouseId,
    Double lat,
    Double lng)
{}
