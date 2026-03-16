package com.fulfillment.orderservice.application.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderReceivedEventPayload(
    String orderId,
    double lat,
    double lng,
    List<Item> items
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String sku, int quantity) {}
}