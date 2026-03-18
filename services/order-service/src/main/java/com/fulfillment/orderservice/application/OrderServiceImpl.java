package com.fulfillment.orderservice.application;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.orderservice.application.dto.CreateOrderCommand;
import com.fulfillment.orderservice.application.dto.OrderReceivedEventPayload;
import com.fulfillment.orderservice.domain.exception.IdempotencyInconsistentStateException;
import com.fulfillment.orderservice.domain.exception.InvalidStatusTransitionException;
import com.fulfillment.orderservice.domain.exception.OrderCreationInProgressException;
import com.fulfillment.orderservice.domain.exception.OrderNotFoundException;
import com.fulfillment.orderservice.domain.exception.OrderNotOwnedException;
import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.domain.model.OrderItem;
import com.fulfillment.orderservice.domain.model.OrderStateHistory;
import com.fulfillment.orderservice.domain.model.Status;
import com.fulfillment.orderservice.domain.ports.IdempotencyStore;
import com.fulfillment.orderservice.domain.ports.OrderRepository;
import com.fulfillment.orderservice.domain.ports.OutboxEventsRepository;
import com.fulfillment.orderservice.domain.ports.OrderWriteTransaction;
import com.fulfillment.orderservice.domain.ports.OrderWriteTransaction.OutboxPendingEvent;

import static com.fulfillment.orderservice.domain.shared.DomainValidations.requireNonBlank;

import java.util.Objects;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final ObjectMapper mapper;
    private final OrderRepository orderRepo;
    private final IdempotencyStore idempotencyStore;
    private final OrderWriteTransaction orderWriteTransaction;
    private final OutboxEventsRepository outboxRepo;

    private static final Duration PENDING_TTL = Duration.ofMinutes(2);
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    public OrderServiceImpl(
            ObjectMapper mapper,
            OrderRepository orderRepo,
            IdempotencyStore idempotencyStore,
            OrderWriteTransaction orderWriteTransaction,
            OutboxEventsRepository outboxRepo) {
        this.mapper = mapper;
        this.orderRepo = orderRepo;
        this.idempotencyStore = idempotencyStore;
        this.orderWriteTransaction = orderWriteTransaction;
        this.outboxRepo = outboxRepo;
    }

    @Override
    public Order create(CreateOrderCommand command, String idempotencyKey) {

        String normalizedKey = requireNonBlank(idempotencyKey, "idempotencyKey").trim();

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

        return Order.createOrder(orderId, command.operatorId(), command.lat(), command.lng(), items);
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
    public Order getById(String orderId, String requesterId, boolean isAdmin) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        assertOwnership(order, requesterId, isAdmin);
        return order;
    }

    @Override
    public void cancel(String orderId, String requesterId, boolean isAdmin) {

        log.info("Requesting order cancellation: orderId={}", orderId);

        Order current = getById(orderId, requesterId, isAdmin);
    
        log.info("Order {} current status={}", orderId, current.getStatus());
    
        if (current.getStatus() == Status.SHIPPED) {
            log.warn("Cannot cancel shipped order: orderId={}", orderId);
            throw new InvalidStatusTransitionException(current.getStatus(), Status.CANCELED);
        }
    
        if (current.getStatus() == Status.CANCELED) {
            log.info("Order already canceled (idempotent): orderId={}", orderId);
            return;
        }

        if (current.getStatus() == Status.REJECTED) {
            log.info("Order already rejected, treating as canceled: orderId={}", orderId);
            return;
        }
        
        String eventType = "OrderCancelled";
        String eventId = "OrderCancelled:" + orderId + ":" + eventType;
    
        OutboxPendingEvent outboxEvent = new OutboxPendingEvent(
            eventId,
            "ORDER",
            orderId,
            eventType,
            buildOrderCancelledPayload(current, "USER_REQUEST")
        );
    
        log.info("Publishing OrderCancelled event (worker will cancel and release inventory): orderId={}, eventId={}", 
                 orderId, eventId);
    
        outboxRepo.savePending(outboxEvent);
    
        log.info("OrderCancelled event published successfully: orderId={}", orderId);
    }
    
    private void assertOwnership(Order order, String requesterId, boolean isAdmin) {
        if (!isAdmin && !Objects.equals(order.getOperatorId(), requesterId)) {
            throw new OrderNotOwnedException(order.getOrderId());
        }
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

    private String buildOrderCancelledPayload(Order order, String reason) {
        try {
            var payload = new com.fulfillment.orderservice.application.dto.OrderCancelledEventPayload(
                order.getOrderId(),
                reason
            );
            return mapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize OrderCancelled payload: " + e.getMessage(), e);
        }
    }
    
}
