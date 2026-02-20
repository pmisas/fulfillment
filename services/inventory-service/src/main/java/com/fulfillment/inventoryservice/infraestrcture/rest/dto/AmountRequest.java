package com.fulfillment.inventoryservice.infraestrcture.rest.dto;

import jakarta.validation.constraints.Positive;

public record AmountRequest(
    @Positive int amount
) {}
