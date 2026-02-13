package com.fulfillment.orderservice.infrastructure.rest.dto;

import java.util.List;

public class OrderResponse {
    
    private final String orderId;
    private final String customerId;
    private final String status;
    private final List<Item> items;

    public OrderResponse(
            String orderId,
            String customerId,
            String status,
            List<Item> items) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
        this.items = items;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getStatus() {
        return status;
    }

    public List<Item> getItems() {
        return items;
    }

    public static class Item {

        private String sku;

        private int quantity;

        public Item(String sku, int quantity) {
            this.sku = sku;
            this.quantity = quantity;
        }

        public String getSku() {
            return sku;
        }

        public int getQuantity() {
            return quantity;
        }
    }
}
