package com.fulfillment.orderservice.application;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.orderservice.application.dto.CreateOrderCommand;
import com.fulfillment.orderservice.application.dto.OrderReceivedEventPayload;
import com.fulfillment.orderservice.domain.exception.IdempotencyInconsistentStateException;
import com.fulfillment.orderservice.domain.exception.OrderNotFoundException;
import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.domain.model.OrderItem;
import com.fulfillment.orderservice.domain.model.OrderStateHistory;
import com.fulfillment.orderservice.domain.ports.IdempotencyStore;
import com.fulfillment.orderservice.domain.ports.OrderRepository;
import com.fulfillment.orderservice.domain.ports.OrderStateHistoryRepository;
import com.fulfillment.orderservice.domain.ports.OutboxEventsRepository;

@Service
public class OrderServiceImpl implements OrderService {
    
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final ObjectMapper mapper = new ObjectMapper();
 
    private final OrderRepository orderRepo;
    private final IdempotencyStore idempotencyStore;
    private final OrderStateHistoryRepository historyRepo;
    private final OutboxEventsRepository outboxRepo;

    public OrderServiceImpl(
            OrderRepository orderRepo, 
            IdempotencyStore idempotencyStore,
            OrderStateHistoryRepository historyRepo,
            OutboxEventsRepository outboxRepo) {
        this.orderRepo = orderRepo;
        this.idempotencyStore = idempotencyStore;
        this.historyRepo = historyRepo;
        this.outboxRepo = outboxRepo;
    }

    @Override
    public Order create(CreateOrderCommand command, String idempotencyKey) {
        
        String normalizedKey = idempotencyKey.trim();
        var existingOrderId = idempotencyStore.getOrderId(normalizedKey);

        if (existingOrderId.isPresent()) {
            String orderId = existingOrderId.get();
            return orderRepo.findById(orderId)
                        .orElseThrow(() -> new 
                    IdempotencyInconsistentStateException(normalizedKey, orderId));
        }

        List<OrderItem> items = command.items().stream()
                        .map(i -> OrderItem.createOrderItem(i.sku(), i.quantity()))
                        .toList();

        Order order = Order.createOrder(
                command.lat(), 
                command.lng(), 
                items
        );

        orderRepo.save(order);
        historyRepo.append(OrderStateHistory.createOrderStateHistory(order.getOrderId()));
        
        String eventId = UUID.randomUUID().toString();
        String payload = buildOrderReceivedPayload(order, command);

        outboxRepo.savePending(
            eventId,
            "ORDER",
            order.getOrderId(),
            "OrderReceived",
            payload
        );

        boolean stored = idempotencyStore.putIfAbsent(normalizedKey, order.getOrderId(), IDEMPOTENCY_TTL);

        if(!stored) {
            String winnerId = idempotencyStore.getOrderId(normalizedKey)
                        .orElse(order.getOrderId());
            return orderRepo.findById(winnerId).orElse(order);
        }

        return order;
    }

    @Override
    public Order getById(String orderId) {
        return orderRepo.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    };

    private String buildOrderReceivedPayload(Order order, CreateOrderCommand command) {
        try {
            var payload = new OrderReceivedEventPayload(
                order.getOrderId(),
                order.getLat(),
                order.getLng(),
                order.getItems().stream()
                    .map(i -> new OrderReceivedEventPayload.Item(i.getSku(), i.getQuantity()))
                    .toList()
            );
            return mapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize OrderReceived payload: " + e.getMessage(), e);
        }
    }
}
