package com.fulfillment.notificationlambda.infrastructure.messaging.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ShipmentDeliveredPayload(
    String orderId,
    String shipmentId) {}
