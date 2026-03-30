package com.fulfillment.orderservice.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.orderservice.domain.exception.IdempotencyInconsistentStateException;
import com.fulfillment.orderservice.domain.exception.InvalidStatusTransitionException;
import com.fulfillment.orderservice.domain.exception.OrderCreationInProgressException;
import com.fulfillment.orderservice.domain.exception.OrderNotFoundException;
import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.domain.model.OrderItem;
import com.fulfillment.orderservice.domain.model.Status;
import com.fulfillment.orderservice.domain.ports.IdempotencyStore;
import com.fulfillment.orderservice.domain.ports.OrderRepository;
import com.fulfillment.orderservice.domain.ports.OrderWriteTransaction;
import com.fulfillment.orderservice.domain.ports.OrderWriteTransaction.OutboxPendingEvent;
import com.fulfillment.orderservice.domain.ports.OutboxEventsRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepo;

    @Mock
    private IdempotencyStore idempotencyStore;

    @Mock
    private OrderWriteTransaction orderWriteTransaction;

    @Mock
    private OutboxEventsRepository outboxRepo;

    private OrderServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        service = new OrderServiceImpl(
            new ObjectMapper(),
            orderRepo,
            idempotencyStore,
            orderWriteTransaction,
            outboxRepo
        );
    }

    @Test
    void create_shouldCreateOrderWhenIdempotencyKeyIsNew() {
        when(idempotencyStore.get("idem-1")).thenReturn(Optional.empty());
        when(idempotencyStore.claimPending(eq("idem-1"), anyString(), any())).thenReturn(true);
        when(idempotencyStore.finalizeOrderId(eq("idem-1"), anyString(), anyString(), any())).thenReturn(true);

        Order result = service.create(
            "operator-1",
            4.7110,
            -74.0721,
            List.of(new OrderService.OrderItemInput("SKU-1", 2)),
            "idem-1"
        );

        assertNotNull(result);
        assertNotNull(result.getOrderId());
        assertEquals(Status.RECEIVED, result.getStatus());
        assertEquals(1, result.getItems().size());
        assertEquals("SKU-1", result.getItems().get(0).getSku());

        verify(orderWriteTransaction).createOrderWithHistoryAndOutbox(
            any(Order.class),
            any(),
            any()
        );

        verify(idempotencyStore).finalizeOrderId(
            eq("idem-1"),
            anyString(),
            eq(result.getOrderId()),
            any()
        );
    }

    @Test
    void create_shouldReturnExistingOrderWhenIdempotencyKeyAlreadyPointsToOrderId() {
        Order existingOrder = Order.createOrder(
            "order-123",
            "operator-1",
            4.7110,
            -74.0721,
            List.of(OrderItem.createOrderItem("SKU-1", 2))
        );

        when(idempotencyStore.get("idem-1")).thenReturn(Optional.of("order-123"));
        when(orderRepo.findById("order-123")).thenReturn(Optional.of(existingOrder));

        Order result = service.create(
            "operator-1",
            4.7110,
            -74.0721,
            List.of(new OrderService.OrderItemInput("SKU-1", 2)),
            "idem-1"
        );

        assertSame(existingOrder, result);
        verify(orderWriteTransaction, never()).createOrderWithHistoryAndOutbox(any(), any(), any());
        verify(idempotencyStore, never()).claimPending(anyString(), anyString(), any());
    }

    @Test
    void create_shouldThrowWhenIdempotencyKeyIsPending() {
        when(idempotencyStore.get("idem-1")).thenReturn(Optional.of("PENDING:token-1"));

        OrderCreationInProgressException exception = assertThrows(
            OrderCreationInProgressException.class,
            () -> service.create(
                "operator-1",
                4.7110,
                -74.0721,
                List.of(new OrderService.OrderItemInput("SKU-1", 2)),
                "idem-1"
            )
        );

        assertNotNull(exception);

        verify(orderWriteTransaction, never()).createOrderWithHistoryAndOutbox(any(), any(), any());
    }

    @Test
    void create_shouldReleaseIdempotencyKeyWhenTransactionFails() {
        when(idempotencyStore.get("idem-1")).thenReturn(Optional.empty());
        when(idempotencyStore.claimPending(eq("idem-1"), anyString(), any())).thenReturn(true);

        doThrow(new RuntimeException("db failure"))
            .when(orderWriteTransaction)
            .createOrderWithHistoryAndOutbox(any(), any(), any());

        RuntimeException ex = assertThrows(
            RuntimeException.class,
            () -> service.create(
                "operator-1",
                4.7110,
                -74.0721,
                List.of(new OrderService.OrderItemInput("SKU-1", 2)),
                "idem-1"
            )
        );
        assertEquals("db failure", ex.getMessage());

        verify(idempotencyStore).release(eq("idem-1"), anyString());
        verify(idempotencyStore, never()).finalizeOrderId(anyString(), anyString(), anyString(), any());
    }

    @Test
    void create_shouldThrowWhenFinalizeFails() {
        when(idempotencyStore.get("idem-1")).thenReturn(Optional.empty());
        when(idempotencyStore.claimPending(eq("idem-1"), anyString(), any())).thenReturn(true);
        when(idempotencyStore.finalizeOrderId(eq("idem-1"), anyString(), anyString(), any())).thenReturn(false);

        IdempotencyInconsistentStateException exception = assertThrows(
            IdempotencyInconsistentStateException.class,
            () -> service.create(
                "operator-1",
                4.7110,
                -74.0721,
                List.of(new OrderService.OrderItemInput("SKU-1", 2)),
                "idem-1"
            )
        );

        assertNotNull(exception);
    }

    @Test
    void getById_shouldReturnOrderWhenExists() {
        Order order = Order.createOrder(
            "order-123",
            "operator-1",
            4.7110,
            -74.0721,
            List.of(OrderItem.createOrderItem("SKU-1", 2))
        );

        when(orderRepo.findById("order-123")).thenReturn(Optional.of(order));

        Order result = service.getById("order-123", "operator-1", false);

        assertSame(order, result);
    }

    @Test
    void getById_shouldThrowWhenOrderDoesNotExist() {
        when(orderRepo.findById("missing")).thenReturn(Optional.empty());

        OrderNotFoundException exception =
            assertThrows(OrderNotFoundException.class, () -> service.getById("missing", "operator-1", false));

        assertNotNull(exception);
    }

    @Test
    void cancel_shouldThrowWhenOrderDoesNotExist() {
        when(orderRepo.findById("missing")).thenReturn(Optional.empty());

        OrderNotFoundException exception =
            assertThrows(OrderNotFoundException.class, () -> service.cancel("missing", "operator-1", false));

        assertNotNull(exception);

        verify(outboxRepo, never()).savePending(any());
    }

    @Test
    void cancel_shouldRejectCancellationWhenOrderIsShipped() {
        Order shipped = mock(Order.class);
        when(shipped.getOperatorId()).thenReturn("operator-1");
        when(shipped.getStatus()).thenReturn(Status.SHIPPED);
        when(orderRepo.findById("order-1")).thenReturn(Optional.of(shipped));

        InvalidStatusTransitionException exception = assertThrows(
            InvalidStatusTransitionException.class,
            () -> service.cancel("order-1", "operator-1", false)
        );

        assertNotNull(exception);

        verify(outboxRepo, never()).savePending(any());
    }

    @Test
    void cancel_shouldRejectCancellationWhenOrderIsPacked() {
        Order packed = mock(Order.class);
        when(packed.getOperatorId()).thenReturn("operator-1");
        when(packed.getStatus()).thenReturn(Status.PACKED);
        when(orderRepo.findById("order-1")).thenReturn(Optional.of(packed));

        InvalidStatusTransitionException exception = assertThrows(
            InvalidStatusTransitionException.class,
            () -> service.cancel("order-1", "operator-1", false)
        );

        assertNotNull(exception);
        verify(outboxRepo, never()).savePending(any());
    }

    @Test
    void cancel_shouldDoNothingWhenOrderAlreadyCanceled() {
        Order canceled = mock(Order.class);
        when(canceled.getOperatorId()).thenReturn("operator-1");
        when(canceled.getStatus()).thenReturn(Status.CANCELED);
        when(orderRepo.findById("order-1")).thenReturn(Optional.of(canceled));

        assertDoesNotThrow(() -> service.cancel("order-1", "operator-1", false));

        verify(outboxRepo, never()).savePending(any());
    }

    @Test
    void cancel_shouldDoNothingWhenOrderAlreadyRejected() {
        Order rejected = mock(Order.class);
        when(rejected.getOperatorId()).thenReturn("operator-1");
        when(rejected.getStatus()).thenReturn(Status.REJECTED);
        when(orderRepo.findById("order-1")).thenReturn(Optional.of(rejected));

        assertDoesNotThrow(() -> service.cancel("order-1", "operator-1", false));

        verify(outboxRepo, never()).savePending(any());
    }

    @Test
    void cancel_shouldPublishOrderCancelledEventWhenOrderIsCancelable() {
        Order validated = mock(Order.class);
        when(validated.getOrderId()).thenReturn("order-1");
        when(validated.getOperatorId()).thenReturn("operator-1");
        when(validated.getStatus()).thenReturn(Status.VALIDATED);
        when(orderRepo.findById("order-1")).thenReturn(Optional.of(validated));

        service.cancel("order-1", "operator-1", false);

        ArgumentCaptor<OutboxPendingEvent> captor = ArgumentCaptor.forClass(OutboxPendingEvent.class);
        verify(outboxRepo).savePending(captor.capture());

        OutboxPendingEvent event = captor.getValue();
        assertEquals("ORDER", event.aggregateType());
        assertEquals("order-1", event.aggregateId());
        assertEquals("OrderCancellationRequested", event.eventType());
        assertTrue(event.eventId().startsWith("OrderCancellationRequested:order-1"));
        assertTrue(event.payload().contains("\"orderId\":\"order-1\""));
        assertTrue(event.payload().contains("\"reason\":\"USER_REQUEST\""));
    }

    @Test
    void cancel_shouldNotPersistOrderDirectly_onlyPublishEvent() {
        Order validated = mock(Order.class);
        when(validated.getOrderId()).thenReturn("order-1");
        when(validated.getOperatorId()).thenReturn("operator-1");
        when(validated.getStatus()).thenReturn(Status.VALIDATED);
        when(orderRepo.findById("order-1")).thenReturn(Optional.of(validated));

        service.cancel("order-1", "operator-1", false);

        verify(outboxRepo).savePending(any());
        verify(orderRepo, never()).save(any());
    }

    @Test
    void cancel_shouldPublishEventWhenOrderIsReceived() {
        Order received = mock(Order.class);
        when(received.getOrderId()).thenReturn("order-1");
        when(received.getOperatorId()).thenReturn("operator-1");
        when(received.getStatus()).thenReturn(Status.RECEIVED);
        when(orderRepo.findById("order-1")).thenReturn(Optional.of(received));

        service.cancel("order-1", "operator-1", false);

        ArgumentCaptor<OutboxPendingEvent> captor = ArgumentCaptor.forClass(OutboxPendingEvent.class);
        verify(outboxRepo).savePending(captor.capture());

        OutboxPendingEvent event = captor.getValue();
        assertEquals("OrderCancellationRequested", event.eventType());
        assertEquals("order-1", event.aggregateId());
        assertTrue(event.payload().contains("\"orderId\":\"order-1\""));
        verify(orderRepo, never()).save(any());
    }
}
