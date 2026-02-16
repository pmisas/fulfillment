package com.fulfillment.orderservice.infrastructure.repository.memory;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.domain.port.OrderRepository;

@Repository
@Profile("local")
public class InMemoryRepositoryAdapter implements OrderRepository{

    private final ConcurrentMap<String, Order> db = new ConcurrentHashMap<>();

    @Override
    public Order save(Order order){
        db.put(order.getOrderId(), order);
        return order;
    };

    @Override
    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(db.get(orderId));
    }
}
