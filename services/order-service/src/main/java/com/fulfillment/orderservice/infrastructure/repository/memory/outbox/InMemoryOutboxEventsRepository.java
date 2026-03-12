package com.fulfillment.orderservice.infrastructure.repository.memory.outbox;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.orderservice.domain.ports.OrderWriteTransaction.OutboxPendingEvent;
import com.fulfillment.orderservice.domain.ports.OutboxEventsRepository;

@Repository
@Profile("local")
public class InMemoryOutboxEventsRepository implements OutboxEventsRepository {

    @Override
    public void savePending(OutboxPendingEvent event) {
        // no-op in local profile: outbox events are not published
    }
}
