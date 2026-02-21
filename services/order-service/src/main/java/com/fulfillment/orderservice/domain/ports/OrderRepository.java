package com.fulfillment.orderservice.domain.ports;

import java.util.Optional;

import com.fulfillment.orderservice.domain.model.Order;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(String orderId);
}
