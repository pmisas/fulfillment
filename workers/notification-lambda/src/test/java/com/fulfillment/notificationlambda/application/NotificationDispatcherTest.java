package com.fulfillment.notificationlambda.application;

import com.fulfillment.notificationlambda.application.handler.EventNotificationHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.*;

class NotificationDispatcherTest {

    private EventNotificationHandler handlerA;
    private NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        handlerA = mock(EventNotificationHandler.class);
        when(handlerA.eventType()).thenReturn("OrderReceived");
        dispatcher = new NotificationDispatcher(Map.of("OrderReceived", handlerA));
    }

    @Test
    void dispatch_shouldDelegateToRegisteredHandler() {
        dispatcher.dispatch("OrderReceived", "{\"orderId\":\"o-1\"}");
        verify(handlerA).handle("{\"orderId\":\"o-1\"}");
    }

    @Test
    void dispatch_shouldIgnoreUnknownEventType() {
        dispatcher.dispatch("UnknownEvent", "{\"orderId\":\"o-1\"}");
        verify(handlerA, never()).handle(any());
    }

    @Test
    void dispatch_shouldIgnoreNullEventType() {
        dispatcher.dispatch(null, "{\"orderId\":\"o-1\"}");
        verify(handlerA, never()).handle(any());
    }
}
