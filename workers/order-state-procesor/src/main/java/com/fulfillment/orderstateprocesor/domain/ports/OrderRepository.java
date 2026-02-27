package com.fulfillment.orderstateprocesor.domain.ports;

import com.fulfillment.orderstateprocesor.domain.model.Order;

import reactor.core.publisher.Mono;

public interface OrderRepository {
    Mono<Order> findById(String orderId);
    Mono<Order> save(Order order);
}
