package com.fulfillment.shippingservice.infrastructure.rest.dto;

import java.time.Instant;

public record ShippingGuideUrlResponse(String url, Instant expiresAt) {}
