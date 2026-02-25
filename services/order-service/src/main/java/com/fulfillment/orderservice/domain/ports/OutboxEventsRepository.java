package com.fulfillment.orderservice.domain.ports;

public interface OutboxEventsRepository {
    void savePending(OrderWriteTransaction.OutboxPendingEvent event);
}
