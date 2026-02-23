/* package com.fulfillment.orderservice.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fulfillment.orderservice.application.dto.CreateOrderCommand;
import com.fulfillment.orderservice.domain.exception.IdempotencyInconsistentStateException;
import com.fulfillment.orderservice.domain.exception.WarehouseNotAvailableException;
import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.domain.model.OrderStateHistory;
import com.fulfillment.orderservice.domain.ports.IdempotencyStore;
import com.fulfillment.orderservice.domain.ports.OrderRepository;
import com.fulfillment.orderservice.domain.ports.OrderStateHistoryRepository;
import com.fulfillment.orderservice.domain.ports.WarehouseClient;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock OrderRepository orderRepo;
    @Mock WarehouseClient warehouseClient;
    @Mock IdempotencyStore idempotencyStore;
    @Mock OrderStateHistoryRepository historyRepo;

    private OrderServiceImpl service;

    private CreateOrderCommand command;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(orderRepo, warehouseClient, idempotencyStore, historyRepo);

        command = new CreateOrderCommand(
                "cust-001",
                List.of(
                        new CreateOrderCommand.Item("SKU-APPLE-01", 2),
                        new CreateOrderCommand.Item("SKU-BANANA-02", 1)
                )
        );
    }

    @Test
    void create_whenNoWarehouse_shouldThrow() {

        when(warehouseClient.anyWarehouseExists()).thenReturn(false);

        assertThrows(WarehouseNotAvailableException.class,
                () -> service.create(command, "idem-123"));

        verifyNoInteractions(orderRepo);
        verifyNoInteractions(idempotencyStore);
        verifyNoInteractions(historyRepo);
    }

    @Test
    void create_whenIdempotencyKeyExists_shouldReturnExistingOrder() {

        when(warehouseClient.anyWarehouseExists()).thenReturn(true);

        String key = "idem-123";
        String orderId = "order-1";

        when(idempotencyStore.getOrderId(key)).thenReturn(Optional.of(orderId));

        Order existing = mock(Order.class);
        when(orderRepo.findById(orderId)).thenReturn(Optional.of(existing));

        Order result = service.create(command, key);

        assertSame(existing, result);

        verify(orderRepo, never()).save(any());
        verify(historyRepo, never()).append(any(OrderStateHistory.class));
        verify(idempotencyStore, never()).putIfAbsent(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void create_whenIdempotencyKeyExistsButOrderMissing_shouldThrowInconsistentState() {

        when(warehouseClient.anyWarehouseExists()).thenReturn(true);

        String key = "idem-123";
        String orderId = "order-missing";

        when(idempotencyStore.getOrderId(key)).thenReturn(Optional.of(orderId));
        when(orderRepo.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(IdempotencyInconsistentStateException.class,
                () -> service.create(command, key));

        verify(orderRepo, never()).save(any());
        verify(historyRepo, never()).append(any());
        verify(idempotencyStore, never()).putIfAbsent(anyString(), anyString(), any());
    }

    @Test
    void create_whenNewKeyAndStoredTrue_shouldSaveOrderAppendHistoryAndStoreIdempotency() {

        when(warehouseClient.anyWarehouseExists()).thenReturn(true);

        String key = "idem-123";
        when(idempotencyStore.getOrderId(key)).thenReturn(Optional.empty());
        when(idempotencyStore.putIfAbsent(eq(key), anyString(), any(Duration.class))).thenReturn(true);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        Order created = service.create(command, key);

        assertNotNull(created);

        verify(orderRepo).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertNotNull(savedOrder.getOrderId());

        verify(historyRepo).append(any(OrderStateHistory.class));
        verify(idempotencyStore).putIfAbsent(eq(key), eq(savedOrder.getOrderId()), any(Duration.class));
    }

    @Test
    void create_whenNewKeyButPutIfAbsentFalse_shouldReturnWinnerOrder() {

        when(warehouseClient.anyWarehouseExists()).thenReturn(true);

        String key = "idem-123";

        when(idempotencyStore.getOrderId(key))
            .thenReturn(Optional.empty())              
            .thenReturn(Optional.of("order-winner")); 

        when(idempotencyStore.putIfAbsent(eq(key), anyString(), any(Duration.class)))
            .thenReturn(false);

        Order winner = mock(Order.class);
        when(orderRepo.findById("order-winner")).thenReturn(Optional.of(winner));

        Order result = service.create(command, key);

        assertSame(winner, result);

        verify(orderRepo, times(1)).save(any(Order.class));
        verify(historyRepo, times(1)).append(any(OrderStateHistory.class));
        verify(orderRepo, times(1)).findById("order-winner");
        verify(idempotencyStore, times(1)).putIfAbsent(eq(key), anyString(), any(Duration.class));
}
}
*/