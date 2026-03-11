package com.fulfillment.shippingservice.application.dto;

public record ShipmentShippedPayload(
    String orderId,
    String shipmentId,
    String trackingId,
    String shippingGuideS3Key,
    String carrier
) {}
