package com.fulfillment.orderservice.infrastructure.repository.memory.order;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.domain.model.Status;
import com.fulfillment.orderservice.domain.ports.OrderRepository;

@Repository
@Profile("local")
public class InMemoryRepositoryAdapter implements OrderRepository {

    private final ConcurrentMap<String, Order> db = new ConcurrentHashMap<>();

    @Override
    public Order save(Order order) {
        db.put(order.getOrderId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(db.get(orderId));
    }

    @Override
    public List<Order> findAll() {
        return List.copyOf(db.values());
    }

    @Override
    public List<Order> findByOperatorId(String operatorId) {
        return db.values().stream()
                .filter(o -> operatorId.equals(o.getOperatorId()))
                .toList();
    }

    @Override
    public List<Order> findByOperatorIdAndStatus(String operatorId, Status status) {
        return db.values().stream()
                .filter(o -> operatorId.equals(o.getOperatorId()) && o.getStatus() == status)
                .toList();
    }

    @Override
    public List<Order> findByOperatorIdAndWarehouseId(String operatorId, String warehouseId) {
        return db.values().stream()
                .filter(o -> operatorId.equals(o.getOperatorId()) && warehouseId.equals(o.getWarehouseId()))
                .toList();
    }

    @Override
    public List<Order> findByStatus(Status status) {
        return db.values().stream()
                .filter(o -> o.getStatus() == status)
                .toList();
    }

    @Override
    public List<Order> findByWarehouseId(String warehouseId) {
        return db.values().stream()
                .filter(o -> warehouseId.equals(o.getWarehouseId()))
                .toList();
    }
}
