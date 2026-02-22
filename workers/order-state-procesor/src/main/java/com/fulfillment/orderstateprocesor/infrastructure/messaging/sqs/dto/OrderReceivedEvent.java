package com.fulfillment.orderstateprocesor.infrastructure.messaging.sqs.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Payload del evento "OrderReceived" que viene en el body del mensaje SQS (JSON).
 *
 * Ejemplo JSON:
 * {
 *   "orderId": "order-123",
 *   "customerId": "cust-1",
 *   "lat": 4.7110,
 *   "lng": -74.0721,
 *   "items": [{"sku":"SKU-1","quantity":2}]
 * }
 */
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