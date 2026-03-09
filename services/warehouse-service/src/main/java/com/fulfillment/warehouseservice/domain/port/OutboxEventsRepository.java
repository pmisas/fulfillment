package com.fulfillment.warehouseservice.domain.port;

public interface OutboxEventsRepository {

    boolean savePendingIfAbsent(OutboxPendingEvent event);

    boolean resetToPendingIfProcessed(String eventId);

    record OutboxPendingEvent(
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload
    ) {}
}
