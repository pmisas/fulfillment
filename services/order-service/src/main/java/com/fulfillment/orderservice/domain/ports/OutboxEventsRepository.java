package com.fulfillment.orderservice.domain.ports;

public interface OutboxEventsRepository {
    void savePending(
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload
    );
}
