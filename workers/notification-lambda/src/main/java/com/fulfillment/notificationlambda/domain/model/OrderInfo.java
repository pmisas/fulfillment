package com.fulfillment.notificationlambda.domain.model;

import java.util.List;

public record OrderInfo(
    String orderId,
    String operatorId,
    List<OrderItem> items) {

    public record OrderItem(String sku, int quantity) {}
}
