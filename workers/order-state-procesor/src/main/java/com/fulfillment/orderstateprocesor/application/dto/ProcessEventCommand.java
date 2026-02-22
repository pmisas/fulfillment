package com.fulfillment.orderstateprocesor.application.dto;

public record ProcessEventCommand(
    String eventId,
    String eventType,
    String payload
) {}