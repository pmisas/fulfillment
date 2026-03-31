package com.fulfillment.orderservice.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.orderservice.domain.exception.InvalidStatusTransitionException;
import com.fulfillment.orderservice.domain.exception.OrderCreationInProgressException;
import com.fulfillment.orderservice.domain.exception.OrderNotOwnedException;
import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.domain.model.OrderItem;
import com.fulfillment.orderservice.domain.model.Status;
import com.fulfillment.orderservice.domain.ports.IdempotencyStore;
import com.fulfillment.orderservice.domain.ports.OrderRepository;
import com.fulfillment.orderservice.domain.ports.OrderWriteTransaction;
import com.fulfillment.orderservice.domain.ports.OutboxEventsRepository;

class OrderServiceImplTest {

    private OrderRepository orderRepo;
    private IdempotencyStore idempotencyStore;
    private OrderWriteTransaction orderWriteTransaction;
    private OutboxEventsRepository outboxRepo;

    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        orderRepo = mock(OrderRepository.class);
        idempotencyStore = mock(IdempotencyStore.class);
        orderWriteTransaction = mock(OrderWriteTransaction.class);
        outboxRepo = mock(OutboxEventsRepository.class);

        service = new OrderServiceImpl(
            new ObjectMapper(),
            orderRepo,
            idempotencyStore,
            orderWriteTransaction,
            outboxRepo
        );
    }

    @Test
    void create_shouldPersistOrderAndInitialHistoryWhenIdempotencyKeyIsNew() {
        when(idempotencyStore.get("idem-1")).thenReturn(Optional.empty());
        when(idempotencyStore.claimPending(anyString(), anyString(), any())).thenReturn(true);
        when(idempotencyStore.finalizeOrderId(anyString(), anyString(), anyString(), any())).thenReturn(true);

        Order result = service.create(
            "operator-1",
            4.7110,
            -74.0721,
            List.of(new OrderService.OrderItemInput("SKU-1", 2)),
            "idem-1"
        );

        assertNotNull(result.getOrderId());
        assertEquals(Status.RECEIVED, result.getStatus());

        verify(orderWriteTransaction).createOrderWithHistoryAndOutbox(any(), any(), any());
        verify(idempotencyStore).finalizeOrderId(anyString(), anyString(), anyString(), any());
    }

    @Test
    void create_shouldThrowWhenIdempotencyKeyIsPending() {
        when(idempotencyStore.get("idem-1")).thenReturn(Optional.of("PENDING:token-1"));

        assertThrows(
            OrderCreationInProgressException.class,
            () -> service.create(
                "operator-1",
                4.7110,
                -74.0721,
                List.of(new OrderService.OrderItemInput("SKU-1", 2)),
                "idem-1"
            )
        );
    }

    @Test
    void getById_shouldRejectAccessWhenRequesterIsNotOwner() {
        Order order = Order.createOrder(
            "order-1",
            "operator-1",
            4.7110,
            -74.0721,
            List.of(OrderItem.createOrderItem("SKU-1", 2))
        );
        when(orderRepo.findById("order-1")).thenReturn(Optional.of(order));

        assertThrows(OrderNotOwnedException.class, () -> service.getById("order-1", "operator-2", false));
    }

    @Test
    void cancel_shouldRejectShippedOrders() {
        Order shipped = mock(Order.class);
        when(shipped.getOperatorId()).thenReturn("operator-1");
        when(shipped.getStatus()).thenReturn(Status.SHIPPED);
        when(orderRepo.findById("order-1")).thenReturn(Optional.of(shipped));

        assertThrows(
            InvalidStatusTransitionException.class,
            () -> service.cancel("order-1", "operator-1", false)
        );

        verify(outboxRepo, never()).savePending(any());
    }

    @Test
    void cancel_shouldPublishCancellationEventForCancelableOrders() {
        Order current = mock(Order.class);
        when(current.getOperatorId()).thenReturn("operator-1");
        when(current.getStatus()).thenReturn(Status.VALIDATED);
        when(current.getOrderId()).thenReturn("order-1");
        when(orderRepo.findById("order-1")).thenReturn(Optional.of(current));

        service.cancel("order-1", "operator-1", false);

        ArgumentCaptor<OrderWriteTransaction.OutboxPendingEvent> captor =
            ArgumentCaptor.forClass(OrderWriteTransaction.OutboxPendingEvent.class);
        verify(outboxRepo).savePending(captor.capture());
        verify(orderWriteTransaction, never()).createOrderWithHistoryAndOutbox(any(), any(), any());

        OrderWriteTransaction.OutboxPendingEvent event = captor.getValue();
        assertEquals("OrderCancellationRequested:order-1", event.eventId());
        assertEquals("ORDER", event.aggregateType());
        assertEquals("order-1", event.aggregateId());
        assertEquals("OrderCancellationRequested", event.eventType());
        assertTrue(event.payload().contains("\"orderId\":\"order-1\""));
    }
}
