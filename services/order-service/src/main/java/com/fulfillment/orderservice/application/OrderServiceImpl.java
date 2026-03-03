package com.fulfillment.orderservice.application;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.orderservice.application.dto.CreateOrderCommand;
import com.fulfillment.orderservice.application.dto.OrderReceivedEventPayload;
import com.fulfillment.orderservice.domain.exception.IdempotencyInconsistentStateException;
import com.fulfillment.orderservice.domain.exception.OrderCreationInProgressException;
import com.fulfillment.orderservice.domain.exception.OrderNotFoundException;
import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.domain.model.OrderItem;
import com.fulfillment.orderservice.domain.model.OrderStateHistory;
import com.fulfillment.orderservice.domain.ports.IdempotencyStore;
import com.fulfillment.orderservice.domain.ports.OrderRepository;
import com.fulfillment.orderservice.domain.ports.OrderWriteTransaction;
import com.fulfillment.orderservice.domain.ports.OrderWriteTransaction.OutboxPendingEvent;

@Service
public class OrderServiceImpl implements OrderService {

    private final ObjectMapper mapper;
    private final OrderRepository orderRepo;
    private final IdempotencyStore idempotencyStore;
    private final OrderWriteTransaction orderWriteTransaction;

    private static final Duration PENDING_TTL = Duration.ofMinutes(2);
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    public OrderServiceImpl(
            ObjectMapper mapper,
            OrderRepository orderRepo,
            IdempotencyStore idempotencyStore,
            OrderWriteTransaction orderWriteTransaction) {
        this.mapper = mapper;
        this.orderRepo = orderRepo;
        this.idempotencyStore = idempotencyStore;
        this.orderWriteTransaction = orderWriteTransaction;
    }

    @Override
    public Order create(CreateOrderCommand command, String idempotencyKey) {

        String normalizedKey = idempotencyKey.trim();

        var existing = idempotencyStore.get(normalizedKey);
        if (existing.isPresent()) {
            return resolveExistingKey(normalizedKey, existing.get());
        }

        return createNewOrder(command, normalizedKey);
    }

    private Order createNewOrder(CreateOrderCommand command, String normalizedKey) {

        String token = UUID.randomUUID().toString();
        boolean claimed = idempotencyStore.claimPending(normalizedKey, token, PENDING_TTL);

        if (!claimed) {
            String storedValue = idempotencyStore.get(normalizedKey).orElseThrow();
            return resolveExistingKey(normalizedKey, storedValue);
        }

        Order order;

        try {
            order = buildNewOrder(command);

            OrderStateHistory history =
                    OrderStateHistory.createOrderStateHistory(
                        UUID.randomUUID().toString(), 
                        order.getOrderId());

            String eventType = "OrderReceived";
            String eventId = "OrderReceived:" + order.getOrderId() + ":" + eventType;

            OutboxPendingEvent outboxEvent = new OutboxPendingEvent(
                    eventId,
                    "ORDER",
                    order.getOrderId(),
                    eventType,
                    buildOrderReceivedPayload(order)
            );

            orderWriteTransaction.createOrderWithHistoryAndOutbox(order, history, outboxEvent);

        } catch (Exception e) {
            idempotencyStore.release(normalizedKey, token);
            throw e;
        }

        boolean finalized = idempotencyStore.finalizeOrderId(
                normalizedKey, token, order.getOrderId(), IDEMPOTENCY_TTL);

        if (!finalized) {
            throw new IdempotencyInconsistentStateException(normalizedKey, order.getOrderId());
        }

        return order;
    }

    private Order buildNewOrder(CreateOrderCommand command) {

        String orderId = UUID.randomUUID().toString();

        List<OrderItem> items = command.items().stream()
                .map(i -> OrderItem.createOrderItem(i.sku(), i.quantity()))
                .toList();

        return Order.createOrder(orderId, command.lat(), command.lng(), items);
    }

    private Order resolveExistingKey(String normalizedKey, String storedValue) {
        if (storedValue.startsWith("PENDING:")) {
            throw new OrderCreationInProgressException(normalizedKey);
        }

        return orderRepo.findById(storedValue)
                .orElseThrow(() ->
                        new IdempotencyInconsistentStateException(normalizedKey, storedValue));
    }

    @Override
    public Order getById(String orderId) {
        return orderRepo.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private String buildOrderReceivedPayload(Order order) {
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
