package com.fulfillment.orderstateprocesor.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fulfillment.orderstateprocesor.application.dto.ProcessEventCommand;
import com.fulfillment.orderstateprocesor.application.handler.OrderEventHandler;

import reactor.core.publisher.Mono;

class OrderStateProcessorServiceImplTest {

    @Test
    void process_shouldDelegateToRegisteredHandler() {
        OrderEventHandler handler = mock(OrderEventHandler.class);
        when(handler.eventType()).thenReturn("OrderReceived");
        when(handler.handle(anyString())).thenReturn(Mono.empty());

        OrderStateProcessorServiceImpl service = new OrderStateProcessorServiceImpl(List.of(handler));

        ProcessEventCommand command = new ProcessEventCommand(
            "evt-1",
            "OrderReceived",
            "{\"orderId\":\"order-1\"}"
        );

        assertDoesNotThrow(() -> service.process(command).block());

        verify(handler).handle("{\"orderId\":\"order-1\"}");
    }

    @Test
    void process_shouldReturnEmptyWithoutErrorForUnknownEventType() {
        OrderStateProcessorServiceImpl service = new OrderStateProcessorServiceImpl(List.of());

        ProcessEventCommand command = new ProcessEventCommand(
            "evt-1",
            "SomeUnknownEvent",
            "{}"
        );

        // Must complete without throwing — unknown events are silently ignored
        assertDoesNotThrow(() -> service.process(command).block());
    }

    @Test
    void process_shouldRouteToCorrectHandlerWhenMultipleHandlersRegistered() {
        OrderEventHandler handler1 = mock(OrderEventHandler.class);
        when(handler1.eventType()).thenReturn("OrderReceived");
        when(handler1.handle(anyString())).thenReturn(Mono.empty());

        OrderEventHandler handler2 = mock(OrderEventHandler.class);
        when(handler2.eventType()).thenReturn("PackingCompleted");
        when(handler2.handle(anyString())).thenReturn(Mono.empty());

        OrderStateProcessorServiceImpl service = new OrderStateProcessorServiceImpl(
            List.of(handler1, handler2)
        );

        ProcessEventCommand command = new ProcessEventCommand(
            "evt-2",
            "PackingCompleted",
            "{\"orderId\":\"order-1\",\"warehouseId\":\"wh-1\"}"
        );

        assertDoesNotThrow(() -> service.process(command).block());

        verify(handler2).handle("{\"orderId\":\"order-1\",\"warehouseId\":\"wh-1\"}");
        verify(handler1, never()).handle(anyString());
    }
}
