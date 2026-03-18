package com.fulfillment.orderservice.domain.ports;

import java.util.List;
import java.util.Optional;

import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.domain.model.Status;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(String orderId);
    List<Order> findAll();
    List<Order> findByOperatorId(String operatorId);
    List<Order> findByOperatorIdAndStatus(String operatorId, Status status);
    List<Order> findByOperatorIdAndWarehouseId(String operatorId, String warehouseId);
    List<Order> findByStatus(Status status);
    List<Order> findByWarehouseId(String warehouseId);
}
