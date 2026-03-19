package com.fulfillment.orderstateprocesor.infrastructure.messaging.sqs.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ShipmentShippedEvent(
    String orderId,
    String shipmentId,
    String trackingId) {
}
