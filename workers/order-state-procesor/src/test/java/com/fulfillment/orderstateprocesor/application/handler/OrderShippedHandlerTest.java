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
import com.fulfillment.orderstateprocesor.domain.ports.OrderRepository;
import com.fulfillment.orderstateprocesor.domain.ports.OrderStateHistoryRepository;

import reactor.core.publisher.Mono;

class OrderShippedHandlerTest {

    private OrderRepository orderRepo;
    private OrderStateHistoryRepository historyRepo;
    private OrderShippedHandler handler;

    @BeforeEach
    void setUp() {
        orderRepo = mock(OrderRepository.class);
        historyRepo = mock(OrderStateHistoryRepository.class);

        handler = new OrderShippedHandler(
            new ObjectMapper(),
            orderRepo,
            historyRepo
        );
    }

    @Test
    void handle_shouldTransitionPackedOrderToShipped() {
        Order packed = Order.restore(
            "order-1",
            "operator-1",
            "wh-1",
            Status.PACKED,
            4.7110,
            -74.0721,
            Instant.now(),
            Instant.now(),
            List.of(OrderItem.createOrderItem("SKU-1", 2))
        );

        when(orderRepo.findById("order-1")).thenReturn(Mono.just(packed));
        when(orderRepo.save(any())).thenReturn(Mono.just(packed.withStatus(Status.SHIPPED)));
        when(historyRepo.append(any())).thenReturn(Mono.empty());

        String payload = """
            {"orderId":"order-1","shipmentId":"ship-1","trackingId":"trk-1"}
            """;

        assertDoesNotThrow(() -> handler.handle(payload).block());

        verify(orderRepo).save(any());
        verify(historyRepo).append(any());
    }

    @Test
    void handle_shouldIgnoreWhenOrderAlreadyShipped() {
        Order shipped = Order.restore(
            "order-1",
            "operator-1",
            "wh-1",
            Status.SHIPPED,
            4.7110,
            -74.0721,
            Instant.now(),
            Instant.now(),
            List.of(OrderItem.createOrderItem("SKU-1", 2))
        );

        when(orderRepo.findById("order-1")).thenReturn(Mono.just(shipped));

        String payload = """
            {"orderId":"order-1","shipmentId":"ship-1","trackingId":"trk-1"}
            """;

        assertDoesNotThrow(() -> handler.handle(payload).block());

        verify(orderRepo, never()).save(any());
        verify(historyRepo, never()).append(any());
    }
}
