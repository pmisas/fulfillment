package com.fulfillment.shippingservice.application.dto;

public record ShipmentDeliveredPayload(
    String orderId,
    String shipmentId
) {}
