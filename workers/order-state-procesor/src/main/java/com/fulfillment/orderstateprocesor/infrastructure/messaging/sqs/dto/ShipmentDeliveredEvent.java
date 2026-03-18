package com.fulfillment.orderstateprocesor.infrastructure.messaging.sqs.dto;

public record ShipmentDeliveredEvent(
    String orderId,
    String shipmentId
) {}
