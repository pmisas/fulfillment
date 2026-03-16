package com.fulfillment.orderservice.application;

import com.fulfillment.orderservice.application.dto.CreateOrderCommand;
import com.fulfillment.orderservice.domain.model.Order;

public interface OrderService {
    Order create(CreateOrderCommand command, String idempotencyKey);
    Order getById(String orderId);
    
    void cancel(String orderId);
}
