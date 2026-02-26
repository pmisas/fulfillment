package com.fulfillment.orderservice.application.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
  guarda como JSON en OutboxEvents.payload
  y luego viaja como MessageBody en SQS para que lo consuma el OrderStateProcessor.
 
  Ej:
  {
    "orderId": "order-123",
    "lat": 4.7110,
    "lng": -74.0721,
    "items": [
      {"sku":"SKU-1","quantity":2}
    ]
  }
*/

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