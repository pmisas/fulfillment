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
    void process_shouldDelegateToMatchingHandler() {
        OrderEventHandler receivedHandler = mock(OrderEventHandler.class);
        when(receivedHandler.eventType()).thenReturn("OrderReceived");
        when(receivedHandler.handle("payload-1")).thenReturn(Mono.empty());

        OrderEventHandler shippedHandler = mock(OrderEventHandler.class);
        when(shippedHandler.eventType()).thenReturn("ShipmentShipped");

        OrderStateProcessorServiceImpl service =
            new OrderStateProcessorServiceImpl(List.of(receivedHandler, shippedHandler));

        ProcessEventCommand command = new ProcessEventCommand("evt-1", "OrderReceived", "payload-1");

        assertDoesNotThrow(() -> service.process(command).block());

        verify(receivedHandler).handle("payload-1");
        verify(shippedHandler, never()).handle(anyString());
    }

    @Test
    void process_shouldDoNothingWhenNoHandlerExists() {
        OrderEventHandler receivedHandler = mock(OrderEventHandler.class);
        when(receivedHandler.eventType()).thenReturn("OrderReceived");

        OrderStateProcessorServiceImpl service =
            new OrderStateProcessorServiceImpl(List.of(receivedHandler));

        ProcessEventCommand command = new ProcessEventCommand("evt-1", "UnknownEvent", "payload-1");

        assertDoesNotThrow(() -> service.process(command).block());

        verify(receivedHandler, never()).handle(anyString());
    }
}
