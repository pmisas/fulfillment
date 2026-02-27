package com.fulfillment.warehouseservice.domain.port;

public interface OutboxEventsRepository {

    /**
     * @return true si se insertó (nuevo), false si ya existía el eventId (idempotente).
     */
    boolean savePendingIfAbsent(OutboxPendingEvent event);

    record OutboxPendingEvent(
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload
    ) {}
}
