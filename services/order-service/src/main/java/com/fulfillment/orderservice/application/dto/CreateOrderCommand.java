package com.fulfillment.orderservice.application.dto;

import java.util.List;


public record CreateOrderCommand(
    String customerId,
    List<Item> items) {
    public record Item(String sku, int quantity) {
    }
}

