package com.fulfillment.orderservice.application.dto;

import java.util.List;


public record CreateOrderCommand(
    String operatorId,
    Double lat,
    Double lng,
    List<Item> items) {
    public record Item(String sku, int quantity) {
    }
}

