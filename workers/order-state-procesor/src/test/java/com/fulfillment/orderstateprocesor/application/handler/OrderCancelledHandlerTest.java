package com.fulfillment.orderstateprocesor.application.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.orderstateprocesor.domain.model.Order;
import com.fulfillment.orderstateprocesor.domain.model.OrderItem;
import com.fulfillment.orderstateprocesor.domain.model.Status;
import com.fulfillment.orderstateprocesor.domain.ports.InventoryClient;
import com.fulfillment.orderstateprocesor.domain.ports.OrderRepository;
import com.fulfillment.orderstateprocesor.domain.ports.OrderStateHistoryRepository;

import reactor.core.publisher.Mono;

class OrderCancelledHandlerTest {

    private OrderRepository orderRepo;
    private OrderStateHistoryRepository historyRepo;
    private InventoryClient inventoryClient;
    private OrderCancelledHandler handler;

    @BeforeEach
    void setUp() {
        orderRepo = mock(OrderRepository.class);
        historyRepo = mock(OrderStateHistoryRepository.class);
        inventoryClient = mock(InventoryClient.class);

        handler = new OrderCancelledHandler(
            new ObjectMapper(),
            orderRepo,
            historyRepo,
            inventoryClient
        );
    }

    @Test
    void handle_shouldReleaseInventoryAndCancelWhenOrderIsValidated() {
        Order order = Order.restore(
            "order-1",
            "wh-1",
            Status.VALIDATED,
            4.7110,
            -74.0721,
            Instant.now(),
            Instant.now(),
            List.of(OrderItem.createOrderItem("SKU-1", 2))
        );

        when(orderRepo.findById("order-1")).thenReturn(Mono.just(order));
        when(inventoryClient.releaseReservation("resv:order-1")).thenReturn(Mono.empty());
        when(orderRepo.saveIfStatusIs(any(), eq(Status.VALIDATED))).thenReturn(Mono.just(true));
        when(historyRepo.append(any())).thenReturn(Mono.empty());

        String payload = "{\"orderId\":\"order-1\",\"reason\":\"USER_REQUEST\"}";

        assertDoesNotThrow(() -> handler.handle(payload).block());

        verify(inventoryClient).releaseReservation("resv:order-1");
        verify(orderRepo).saveIfStatusIs(
            argThat(o -> o.getStatus() == Status.CANCELED),
            eq(Status.VALIDATED)
        );
        verify(historyRepo).append(any());
    }

    @Test
    void handle_shouldReleaseInventoryAndCancelWhenOrderIsReceived() {
        Order order = Order.restore(
            "order-1",
            null,
            Status.RECEIVED,
            4.7110,
            -74.0721,
            Instant.now(),
            Instant.now(),
            List.of(OrderItem.createOrderItem("SKU-1", 2))
        );

        when(orderRepo.findById("order-1")).thenReturn(Mono.just(order));
        when(inventoryClient.releaseReservation("resv:order-1")).thenReturn(Mono.empty());
        when(orderRepo.saveIfStatusIs(any(), eq(Status.RECEIVED))).thenReturn(Mono.just(true));
        when(historyRepo.append(any())).thenReturn(Mono.empty());

        String payload = "{\"orderId\":\"order-1\",\"reason\":\"USER_REQUEST\"}";

        assertDoesNotThrow(() -> handler.handle(payload).block());

        verify(inventoryClient).releaseReservation("resv:order-1");
        verify(orderRepo).saveIfStatusIs(
            argThat(o -> o.getStatus() == Status.CANCELED),
            eq(Status.RECEIVED)
        );
        verify(historyRepo).append(any());
    }

    @Test
    void handle_shouldAttemptInventoryReleaseButNotSaveWhenOrderAlreadyCanceled() {
        Order order = Order.restore(
            "order-1",
            "wh-1",
            Status.CANCELED,
            4.7110,
            -74.0721,
            Instant.now(),
            Instant.now(),
            List.of(OrderItem.createOrderItem("SKU-1", 2))
        );

        when(orderRepo.findById("order-1")).thenReturn(Mono.just(order));
        when(inventoryClient.releaseReservation("resv:order-1")).thenReturn(Mono.empty());

        String payload = "{\"orderId\":\"order-1\",\"reason\":\"USER_REQUEST\"}";

        assertDoesNotThrow(() -> handler.handle(payload).block());

        verify(inventoryClient).releaseReservation("resv:order-1");
        verify(orderRepo, never()).saveIfStatusIs(any(), any());
        verify(historyRepo, never()).append(any());
    }

    @Test
    void handle_shouldSkipEverythingWhenOrderIsShipped() {
        Order order = Order.restore(
            "order-1",
            "wh-1",
            Status.SHIPPED,
            4.7110,
            -74.0721,
            Instant.now(),
            Instant.now(),
            List.of(OrderItem.createOrderItem("SKU-1", 2))
        );

        when(orderRepo.findById("order-1")).thenReturn(Mono.just(order));

        String payload = "{\"orderId\":\"order-1\",\"reason\":\"USER_REQUEST\"}";

        assertDoesNotThrow(() -> handler.handle(payload).block());

        verify(inventoryClient, never()).releaseReservation(anyString());
        verify(orderRepo, never()).saveIfStatusIs(any(), any());
        verify(historyRepo, never()).append(any());
    }

    @Test
    void handle_shouldSkipEverythingWhenOrderIsRejected() {
        Order order = Order.restore(
            "order-1",
            null,
            Status.REJECTED,
            4.7110,
            -74.0721,
            Instant.now(),
            Instant.now(),
            List.of(OrderItem.createOrderItem("SKU-1", 2))
        );

        when(orderRepo.findById("order-1")).thenReturn(Mono.just(order));

        String payload = "{\"orderId\":\"order-1\",\"reason\":\"USER_REQUEST\"}";

        assertDoesNotThrow(() -> handler.handle(payload).block());

        verify(inventoryClient, never()).releaseReservation(anyString());
        verify(orderRepo, never()).saveIfStatusIs(any(), any());
        verify(historyRepo, never()).append(any());
    }

    @Test
    void handle_shouldSwallowInventoryClientErrorAndStillCancelOrder() {
        Order order = Order.restore(
            "order-1",
            "wh-1",
            Status.PICKED,
            4.7110,
            -74.0721,
            Instant.now(),
            Instant.now(),
            List.of(OrderItem.createOrderItem("SKU-1", 2))
        );

        when(orderRepo.findById("order-1")).thenReturn(Mono.just(order));
        when(inventoryClient.releaseReservation("resv:order-1"))
            .thenReturn(Mono.error(new RuntimeException("reservation not found")));
        when(orderRepo.saveIfStatusIs(any(), eq(Status.PICKED))).thenReturn(Mono.just(true));
        when(historyRepo.append(any())).thenReturn(Mono.empty());

        String payload = "{\"orderId\":\"order-1\",\"reason\":\"USER_REQUEST\"}";

        assertDoesNotThrow(() -> handler.handle(payload).block());

        verify(orderRepo).saveIfStatusIs(
            argThat(o -> o.getStatus() == Status.CANCELED),
            eq(Status.PICKED)
        );
        verify(historyRepo).append(any());
    }

    @Test
    void handle_shouldNotAppendHistoryWhenSaveReturnedFalse() {
        Order order = Order.restore(
            "order-1",
            "wh-1",
            Status.VALIDATED,
            4.7110,
            -74.0721,
            Instant.now(),
            Instant.now(),
            List.of(OrderItem.createOrderItem("SKU-1", 2))
        );

        when(orderRepo.findById("order-1")).thenReturn(Mono.just(order));
        when(inventoryClient.releaseReservation("resv:order-1")).thenReturn(Mono.empty());
        when(orderRepo.saveIfStatusIs(any(), eq(Status.VALIDATED))).thenReturn(Mono.just(false));

        String payload = "{\"orderId\":\"order-1\",\"reason\":\"USER_REQUEST\"}";

        assertDoesNotThrow(() -> handler.handle(payload).block());

        verify(orderRepo).saveIfStatusIs(any(), eq(Status.VALIDATED));
        verify(historyRepo, never()).append(any());
    }
}
