package com.fulfillment.orderservice.application;

import java.util.List;

import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.domain.model.Status;

public interface OrderService {

    Order create(
        String operatorId,
        Double lat,
        Double lng,
        List<OrderItemInput> items,
        String idempotencyKey
    );

    Order getById(String orderId, String requesterId, boolean isAdmin);

    void cancel(String orderId, String requesterId, boolean isAdmin);

    List<Order> listAll(String requesterId, boolean isAdmin);

    List<Order> listByStatus(Status status, String requesterId, boolean isAdmin);

    List<Order> listByWarehouse(String warehouseId, String requesterId, boolean isAdmin);

    List<Order> listByOperator(String operatorId, String requesterId, boolean isAdmin);



    record OrderItemInput(String sku, int quantity) {}
}
