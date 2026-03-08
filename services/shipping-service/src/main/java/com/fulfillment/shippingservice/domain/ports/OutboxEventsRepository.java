package com.fulfillment.shippingservice.domain.ports;

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
