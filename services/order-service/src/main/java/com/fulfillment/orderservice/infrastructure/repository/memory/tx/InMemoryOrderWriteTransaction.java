package com.fulfillment.orderservice.infrastructure.repository.memory.tx;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.domain.model.OrderStateHistory;
import com.fulfillment.orderservice.domain.ports.OrderRepository;
import com.fulfillment.orderservice.domain.ports.OrderStateHistoryRepository;
import com.fulfillment.orderservice.domain.ports.OrderWriteTransaction;
import com.fulfillment.orderservice.domain.ports.OutboxEventsRepository;

@Component
@Profile("local")
public class InMemoryOrderWriteTransaction implements OrderWriteTransaction {

    private final OrderRepository orderRepository;
    private final OrderStateHistoryRepository historyRepository;
    private final OutboxEventsRepository outboxRepository;

    public InMemoryOrderWriteTransaction(
            OrderRepository orderRepository,
            OrderStateHistoryRepository historyRepository,
            OutboxEventsRepository outboxRepository) {
        this.orderRepository = orderRepository;
        this.historyRepository = historyRepository;
        this.outboxRepository = outboxRepository;
    }

    @Override
    public void createOrderWithHistoryAndOutbox(
            Order order,
            OrderStateHistory initialHistory,
            OutboxPendingEvent outboxEvent) {
        orderRepository.save(order);
        historyRepository.append(initialHistory);
        outboxRepository.savePending(outboxEvent);
    }

    @Override
    public void updateOrderWithHistoryAndOutbox(
            Order order,
            OrderStateHistory history,
            OutboxPendingEvent outboxEvent) {
        orderRepository.save(order);
        historyRepository.append(history);
        outboxRepository.savePending(outboxEvent);
    }
}
