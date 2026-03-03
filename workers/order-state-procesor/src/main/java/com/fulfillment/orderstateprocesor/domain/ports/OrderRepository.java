package com.fulfillment.orderstateprocesor.domain.ports;

import com.fulfillment.orderstateprocesor.domain.model.Order;
import com.fulfillment.orderstateprocesor.domain.model.Status;

import reactor.core.publisher.Mono;

public interface OrderRepository {
    Mono<Order> findById(String orderId);
    Mono<Order> save(Order order);
    
    /**
     * @return 
     */
    Mono<Boolean> saveIfStatusIs(Order order, Status expectedStatus);
}
