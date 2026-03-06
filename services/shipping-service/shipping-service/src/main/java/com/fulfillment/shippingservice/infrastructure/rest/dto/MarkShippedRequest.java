package com.fulfillment.shippingservice.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record MarkShippedRequest(@NotBlank String trackingId) {
}
