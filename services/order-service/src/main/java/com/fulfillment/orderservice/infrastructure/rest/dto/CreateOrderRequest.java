package com.fulfillment.orderservice.infrastructure.rest.dto;

import java.util.List;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public class CreateOrderRequest {
    
    @NotBlank
    private String customerId;

    @NotEmpty
    @Valid
    private List<Item> items;


    public String getCustomerId() { 
        return customerId;
    }

    public List<Item> getItems() {
        return items;
    }

    public static class Item {

        @NotBlank
        private String sku;

        @Min(1)
        private int quantity;

        public String getSku() {
            return sku;
        }

        public int getQuantity() {
            return quantity;
        }
    }
}
