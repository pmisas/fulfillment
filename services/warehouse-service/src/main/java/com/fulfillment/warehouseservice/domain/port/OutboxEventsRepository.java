package com.fulfillment.warehouseservice.domain.port;

public interface OutboxEventsRepository {
    void savePending(OutboxPendingEvent event);

    record OutboxPendingEvent(
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload
    ) {}
}
