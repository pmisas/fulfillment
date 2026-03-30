package com.fulfillment.orderservice.infrastructure.rest;

import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.infrastructure.rest.dto.OrderResponse;

public final class OrderRestMapper {
    
    private OrderRestMapper() {}

    public static OrderResponse toResponse(Order order) {
        return new OrderResponse(
            order.getOrderId(),
            order.getStatus().name()
        );
    }

}
