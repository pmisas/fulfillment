package com.fulfillment.orderservice.domain.ports;

import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.domain.model.OrderStateHistory;

public interface OrderWriteTransaction {
    void createOrderWithHistoryAndOutbox(
        Order order,
        OrderStateHistory initialHistory,
        OutboxPendingEvent outboxEvent
    );

    record OutboxPendingEvent(
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload
    ) {}
}
