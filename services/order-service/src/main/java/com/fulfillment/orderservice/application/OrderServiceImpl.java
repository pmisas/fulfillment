package com.fulfillment.orderservice.application;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fulfillment.orderservice.application.dto.CreateOrderCommand;
import com.fulfillment.orderservice.domain.exception.OrderNotFoundException;
import com.fulfillment.orderservice.domain.exception.WarehouseNotAvailableException;
import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.domain.port.OrderRepository;
import com.fulfillment.orderservice.domain.port.WarehouseClient;
import com.fulfillment.orderservice.domain.model.OrderItem;

@Service
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository repo;
    private final WarehouseClient warehouseClient;

    public OrderServiceImpl(OrderRepository repo, WarehouseClient warehouseClient) {
        this.repo = repo;
        this.warehouseClient = warehouseClient;
    }

    @Override
    public Order create(CreateOrderCommand command, String idempotencyKey) {
        //comprobar idempotencia
        
        if(!warehouseClient.anyWarehouseExists()) {
            throw new WarehouseNotAvailableException();
        }

        List<OrderItem> items = command.items().stream()
                        .map(i -> OrderItem.createOrderItem(i.sku(), i.quantity()))
                        .toList();

        Order order = Order.createOrder("123werehouse" , command.customerId(), items);
        repo.save(order);
        return order;
    }

    @Override
    public Order getById(String orderId) {
        return repo.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    };
}
