package com.fulfillment.warehouseservice.infrastructure.repository.memory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.warehouseservice.domain.port.OutboxEventsRepository;

@Repository
@Profile("local")
public class InMemoryOutboxEventsRepository implements OutboxEventsRepository {

    private final Set<String> processedEvents = ConcurrentHashMap.newKeySet();

    @Override
    public boolean savePendingIfAbsent(OutboxPendingEvent event) {
        return true;
    }

    @Override
    public boolean resetToPendingIfProcessed(String eventId) {
        return false;
    }
}
