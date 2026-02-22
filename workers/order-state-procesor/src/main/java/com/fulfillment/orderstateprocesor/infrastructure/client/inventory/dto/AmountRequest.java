package com.fulfillment.orderstateprocesor.infrastructure.client.inventory.dto;

import jakarta.validation.constraints.Positive;

public record AmountRequest(@Positive int amount) {}