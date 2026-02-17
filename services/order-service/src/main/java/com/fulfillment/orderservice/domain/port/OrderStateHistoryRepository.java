package com.fulfillment.orderservice.domain.port;

import java.util.List;

import com.fulfillment.orderservice.domain.model.OrderStateHistory;

public interface OrderStateHistoryRepository {
    void append(OrderStateHistory history);
    List<OrderStateHistory> findByOrderId(String OrderId);
}
