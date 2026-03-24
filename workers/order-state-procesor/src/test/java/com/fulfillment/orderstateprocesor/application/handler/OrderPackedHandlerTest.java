package com.fulfillment.orderstateprocesor.application.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.fulfillment.orderstateprocesor.domain.ports.ShippingClient;

import reactor.core.publisher.Mono;

class OrderPackedHandlerTest {

    private OrderRepository orderRepo;
    private OrderStateHistoryRepository historyRepo;
    private InventoryClient inventoryClient;
    private ShippingClient shippingClient;
    private OrderPackedHandler handler;

    @BeforeEach
    void setUp() {
        orderRepo = mock(OrderRepository.class);
        historyRepo = mock(OrderStateHistoryRepository.class);
        inventoryClient = mock(InventoryClient.class);
        shippingClient = mock(ShippingClient.class);

        handler = new OrderPackedHandler(
            new ObjectMapper(),
            orderRepo,
            historyRepo,
            inventoryClient,
            shippingClient
        );
    }

    @Test
    void handle_shouldConsumeReservationCreateShipmentAndTransitionToPacked() {
        Order order = Order.restore(
            "order-1",
            "operator-1",
            "wh-1",
            Status.PICKED,
            4.7110,
            -74.0721,
            Instant.now(),
            Instant.now(),
            List.of(OrderItem.createOrderItem("SKU-1", 2))
        );

        when(orderRepo.findById("order-1")).thenReturn(Mono.just(order));
        when(inventoryClient.consumeReservation("resv:order-1"))
            .thenReturn(Mono.just(InventoryClient.ConsumeResult.CONSUMED));
        when(shippingClient.createShipment(eq("order-1"), eq("wh-1"), anyList()))
            .thenReturn(Mono.empty());
        when(orderRepo.saveIfStatusIs(any(), eq(Status.PICKED))).thenReturn(Mono.just(true));
        when(historyRepo.append(any())).thenReturn(Mono.empty());

        String payload = """
            {"orderId":"order-1","warehouseId":"wh-1"}
            """;

        assertDoesNotThrow(() -> handler.handle(payload).block());

        verify(inventoryClient).consumeReservation("resv:order-1");
        verify(shippingClient).createShipment(eq("order-1"), eq("wh-1"), anyList());
        verify(orderRepo).saveIfStatusIs(any(), eq(Status.PICKED));
        verify(historyRepo).append(any());
    }

    @Test
    void handle_shouldIgnoreWhenOrderIsNotPicked() {
        Order order = Order.restore(
            "order-1",
            "operator-1",
            "wh-1",
            Status.VALIDATED,
            4.7110,
            -74.0721,
            Instant.now(),
            Instant.now(),
            List.of(OrderItem.createOrderItem("SKU-1", 2))
        );

        when(orderRepo.findById("order-1")).thenReturn(Mono.just(order));

        String payload = """
            {"orderId":"order-1","warehouseId":"wh-1"}
            """;

        assertDoesNotThrow(() -> handler.handle(payload).block());

        verify(inventoryClient, never()).consumeReservation(anyString());
        verify(shippingClient, never()).createShipment(anyString(), anyString(), anyList());
    }

    @Test
    void handle_shouldIgnoreWhenWarehouseIdDoesNotMatch() {
        Order order = Order.restore(
            "order-1",
            "operator-1",
            "wh-1",
            Status.PICKED,
            4.7110,
            -74.0721,
            Instant.now(),
            Instant.now(),
            List.of(OrderItem.createOrderItem("SKU-1", 2))
        );

        when(orderRepo.findById("order-1")).thenReturn(Mono.just(order));

        String payload = """
            {"orderId":"order-1","warehouseId":"wh-2"}
            """;

        assertDoesNotThrow(() -> handler.handle(payload).block());

        verify(inventoryClient, never()).consumeReservation(anyString());
        verify(shippingClient, never()).createShipment(anyString(), anyString(), anyList());
        verify(orderRepo, never()).saveIfStatusIs(any(), any());
        verify(historyRepo, never()).append(any());
    }

    @Test
    void handle_shouldNotCreateShipmentWhenReservationWasNotConsumed() {
        Order order = Order.restore(
            "order-1",
            "operator-1",
            "wh-1",
            Status.PICKED,
            4.7110,
            -74.0721,
            Instant.now(),
            Instant.now(),
            List.of(OrderItem.createOrderItem("SKU-1", 2))
        );

        when(orderRepo.findById("order-1")).thenReturn(Mono.just(order));
        when(orderRepo.saveIfStatusIs(any(), eq(Status.PICKED))).thenReturn(Mono.just(true));
        when(inventoryClient.consumeReservation("resv:order-1"))
            .thenReturn(Mono.just(InventoryClient.ConsumeResult.RESERVATION_NOT_FOUND));

        String payload = """
            {"orderId":"order-1","warehouseId":"wh-1"}
            """;

        assertThrows(IllegalStateException.class, () -> handler.handle(payload).block());

        verify(shippingClient, never()).createShipment(anyString(), anyString(), anyList());
        verify(historyRepo, never()).append(any());
    }
}
