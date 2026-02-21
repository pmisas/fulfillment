package com.fulfillment.orderservice.infrastructure.repository.memory.history;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.orderservice.domain.model.OrderStateHistory;
import com.fulfillment.orderservice.domain.ports.OrderStateHistoryRepository;

@Repository
@Profile("local")
public class InMemoryOrderStateHistoryRepositoryAdapter implements OrderStateHistoryRepository {

    private final ConcurrentMap<String, List<OrderStateHistory>> storage = new ConcurrentHashMap<>();

    @Override
    public void append(OrderStateHistory history) {
        storage.compute(history.getOrderId(), (orderId, existingList) -> {
            if (existingList == null) {
                existingList = new ArrayList<>();
            }
            existingList.add(history);
            return existingList;
        });
    }

    @Override
    public List<OrderStateHistory> findByOrderId(String orderId) {
        return storage.getOrDefault(orderId, List.of());
    }
}
