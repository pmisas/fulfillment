package com.fulfillment.orderstateprocesor.infrastructure.messaging.sqs.dto;

public record ShipmentShippedEvent(
    String orderId,
    String shipmentId,
    String trackingId) {
}
