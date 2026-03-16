package com.fulfillment.orderstateprocesor.infrastructure.messaging.sqs.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderReceivedEvent(
    String orderId,
    String customerId,
    double lat,
    double lng,
    List<Item> items
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String sku, int quantity) {}
}