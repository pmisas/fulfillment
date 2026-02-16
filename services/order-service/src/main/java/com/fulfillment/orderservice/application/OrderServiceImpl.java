package com.fulfillment.orderservice.application;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fulfillment.orderservice.application.dto.CreateOrderCommand;
import com.fulfillment.orderservice.domain.exception.IdempotencyInconsistentStateException;
import com.fulfillment.orderservice.domain.exception.OrderNotFoundException;
import com.fulfillment.orderservice.domain.exception.WarehouseNotAvailableException;
import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.domain.port.IdempotencyStore;
import com.fulfillment.orderservice.domain.port.OrderRepository;
import com.fulfillment.orderservice.domain.port.WarehouseClient;
import com.fulfillment.orderservice.domain.model.OrderItem;

@Service
public class OrderServiceImpl implements OrderService {
    
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final OrderRepository repo;
    private final WarehouseClient warehouseClient;
    private final IdempotencyStore idempotencyStore;

    public OrderServiceImpl(
            OrderRepository repo, 
            WarehouseClient warehouseClient, 
            IdempotencyStore idempotencyStore) {
        this.repo = repo;
        this.warehouseClient = warehouseClient;
        this.idempotencyStore = idempotencyStore;
    }

    @Override
    public Order create(CreateOrderCommand command, String idempotencyKey) {
        
        if(!warehouseClient.anyWarehouseExists()) {
            throw new WarehouseNotAvailableException();
        }
        
        String normalizedKey = idempotencyKey.trim();

        var existingOrderId = idempotencyStore.getOrderId(normalizedKey);

        if (existingOrderId.isPresent()) {
            String orderId = existingOrderId.get();
            return repo.findById(orderId)
                        .orElseThrow(() -> new 
                    IdempotencyInconsistentStateException(normalizedKey, orderId));
        }

        List<OrderItem> items = command.items().stream()
                        .map(i -> OrderItem.createOrderItem(i.sku(), i.quantity()))
                        .toList();

        Order order = Order.createOrder("123werehouse" , command.customerId(), items);
        repo.save(order);

        boolean stored = idempotencyStore.putIfAbsent(normalizedKey, order.getOrderId(), IDEMPOTENCY_TTL);

        if(!stored) {
            String winnerId = idempotencyStore.getOrderId(normalizedKey)
                        .orElse(order.getOrderId());
            return repo.findById(winnerId).orElse(order);
        }

        return order;
    }

    @Override
    public Order getById(String orderId) {
        return repo.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    };
}
