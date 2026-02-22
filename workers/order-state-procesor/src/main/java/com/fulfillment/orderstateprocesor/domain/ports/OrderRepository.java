package com.fulfillment.orderstateprocesor.domain.ports;

import java.util.Optional;
import com.fulfillment.orderstateprocesor.domain.model.Order;

public interface OrderRepository {
    Optional<Order> findById(String orderId);
    Order save(Order order);
}