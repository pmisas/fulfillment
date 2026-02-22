package com.fulfillment.orderstateprocesor.infrastructure.messaging.sqs.dto;

public record OrderEventMessage(
    String eventId,
    String eventType,
    String payload
) {}
