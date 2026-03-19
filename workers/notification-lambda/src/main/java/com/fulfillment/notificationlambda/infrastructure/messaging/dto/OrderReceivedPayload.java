package com.fulfillment.notificationlambda.infrastructure.messaging.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderReceivedPayload(
    String orderId,
    double lat,
    double lng,
    List<Item> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String sku, int quantity) {}
}
