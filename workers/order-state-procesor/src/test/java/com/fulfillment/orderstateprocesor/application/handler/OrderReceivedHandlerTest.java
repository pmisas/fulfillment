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
import com.fulfillment.orderstateprocesor.domain.ports.WarehouseClient;

import reactor.core.publisher.Mono;

class OrderReceivedHandlerTest {

    private OrderRepository orderRepo;
    private OrderStateHistoryRepository historyRepo;
    private WarehouseClient warehouseClient;
    private InventoryClient inventoryClient;
    private OrderReceivedHandler handler;

    @BeforeEach
    void setUp() {
        orderRepo = mock(OrderRepository.class);
        historyRepo = mock(OrderStateHistoryRepository.class);
        warehouseClient = mock(WarehouseClient.class);
        inventoryClient = mock(InventoryClient.class);

        handler = new OrderReceivedHandler(
            new ObjectMapper(),
            orderRepo,
            historyRepo,
            warehouseClient,
            inventoryClient
        );
    }

    @Test
    void handle_shouldValidateOrderWhenWarehouseCanReserve() {
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
        when(warehouseClient.listWarehouses()).thenReturn(Mono.just(List.of(
            new WarehouseClient.WarehouseSummary("wh-1", 4.70, -74.07)
        )));
        when(inventoryClient.checkAvailability(eq("wh-1"), anyList())).thenReturn(Mono.just(
            new InventoryClient.AvailabilityResult(
                true,
                List.of(new InventoryClient.ItemAvailability("SKU-1", 2, 10, true))
            )
        ));
        when(inventoryClient.reserveAll(eq("resv:order-1"), eq("order-1"), eq("wh-1"), anyList()))
            .thenReturn(Mono.just(InventoryClient.ReserveResult.RESERVED));
        when(orderRepo.saveIfStatusIs(any(), eq(Status.RECEIVED))).thenReturn(Mono.just(true));
        when(historyRepo.append(any())).thenReturn(Mono.empty());

        String payload = """
            {"orderId":"order-1","lat":4.7110,"lng":-74.0721,"items":[{"sku":"SKU-1","quantity":2}]}
            """;

        assertDoesNotThrow(() -> handler.handle(payload).block());

        verify(inventoryClient).reserveAll(eq("resv:order-1"), eq("order-1"), eq("wh-1"), anyList());
        verify(orderRepo).saveIfStatusIs(any(), eq(Status.RECEIVED));
        verify(historyRepo).append(any());
    }

    @Test
    void handle_shouldRejectOrderWhenNoWarehouseCanReserve() {
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
        when(warehouseClient.listWarehouses()).thenReturn(Mono.just(List.of(
            new WarehouseClient.WarehouseSummary("wh-1", 4.70, -74.07)
        )));
        when(inventoryClient.checkAvailability(eq("wh-1"), anyList())).thenReturn(Mono.just(
            new InventoryClient.AvailabilityResult(
                false,
                List.of(new InventoryClient.ItemAvailability("SKU-1", 2, 0, false))
            )
        ));
        when(orderRepo.saveIfStatusIs(any(), eq(Status.RECEIVED))).thenReturn(Mono.just(true));
        when(historyRepo.append(any())).thenReturn(Mono.empty());

        String payload = """
            {"orderId":"order-1","lat":4.7110,"lng":-74.0721,"items":[{"sku":"SKU-1","quantity":2}]}
            """;

        assertDoesNotThrow(() -> handler.handle(payload).block());

        verify(inventoryClient, never()).reserveAll(anyString(), anyString(), anyString(), anyList());
        verify(orderRepo).saveIfStatusIs(any(), eq(Status.RECEIVED));
        verify(historyRepo).append(any());
    }
}
