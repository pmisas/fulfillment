package com.fulfillment.orderservice.application;

import com.fulfillment.orderservice.application.dto.CreateOrderCommand;
import com.fulfillment.orderservice.domain.exception.OrderNotFoundException;
import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.domain.port.OrderRepository;

public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository repo;

    public OrderServiceImpl(OrderRepository repo) {
        this.repo = repo;
    }

    @Override
    public Order create(CreateOrderCommand command, String idempotencyKey) {
        //comprobar que existe werehouse
        //comprobar idempotencia
        
        Order order = Order.createOrder("123werehouse" , command.customerId());
        repo.save(order);
        return order;
    }

    @Override
    public Order getById(String orderId) {
        return repo.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    };
}
