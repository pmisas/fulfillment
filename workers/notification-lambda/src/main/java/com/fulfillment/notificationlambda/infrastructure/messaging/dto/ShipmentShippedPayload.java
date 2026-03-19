package com.fulfillment.notificationlambda.infrastructure.messaging.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ShipmentShippedPayload(
    String orderId,
    String shipmentId,
    String trackingId,
    String carrier,
    String estimatedDeliveryAt) {}
