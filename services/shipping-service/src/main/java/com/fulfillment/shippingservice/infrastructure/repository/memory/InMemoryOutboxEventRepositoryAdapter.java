package com.fulfillment.shippingservice.infrastructure.repository.memory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.shippingservice.domain.ports.OutboxEventsRepository;

@Repository
@Profile("local")
public class InMemoryOutboxEventRepositoryAdapter implements OutboxEventsRepository {

    private final Set<String> eventIds = ConcurrentHashMap.newKeySet();

    @Override
    public boolean savePendingIfAbsent(OutboxPendingEvent event) {
        return eventIds.add(event.eventId());
    }

    @Override
    public boolean resetToPendingIfProcessed(String eventId) {
        return eventIds.contains(eventId);
    }
}
