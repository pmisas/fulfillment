package com.fulfillment.orderstateprocesor.domain.ports;

import com.fulfillment.orderstateprocesor.domain.model.OrderStateHistory;

public interface OrderStateHistoryRepository {
    void append(OrderStateHistory history);
}
