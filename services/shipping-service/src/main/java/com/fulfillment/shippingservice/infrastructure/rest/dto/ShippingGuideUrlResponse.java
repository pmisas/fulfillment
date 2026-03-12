package com.fulfillment.shippingservice.infrastructure.rest.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "URL pre-firmada de la guía de despacho")
public record ShippingGuideUrlResponse(
    @Schema(description = "URL pre-firmada de S3 para descargar la guía", example = "https://s3.amazonaws.com/...")
    String url,
    @Schema(description = "Timestamp de expiración del enlace (15 minutos desde la solicitud)")
    Instant expiresAt) {}

