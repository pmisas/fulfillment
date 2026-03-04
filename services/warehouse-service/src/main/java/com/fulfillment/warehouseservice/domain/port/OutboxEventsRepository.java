package com.fulfillment.warehouseservice.domain.port;

public interface OutboxEventsRepository {

    /**
     * @return true si se insertó (nuevo), false si ya existía el eventId (idempotente).
     */
    boolean savePendingIfAbsent(OutboxPendingEvent event);

    /**
     * Resetea un evento existente a PENDING para re-procesamiento.
     * Útil cuando se llama de nuevo a un endpoint y se necesita re-enviar el evento.
     * @return true si se actualizó, false si el evento no existe o ya está PENDING
     */
    boolean resetToPendingIfProcessed(String eventId);

    record OutboxPendingEvent(
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload
    ) {}
}
