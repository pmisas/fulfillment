package com.fulfillment.notificationlambda.application.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.notificationlambda.domain.model.EmailNotification;
import com.fulfillment.notificationlambda.domain.model.OrderInfo;
import com.fulfillment.notificationlambda.domain.ports.EmailSender;
import com.fulfillment.notificationlambda.domain.ports.OperatorEmailLookup;
import com.fulfillment.notificationlambda.domain.ports.OrderLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderReceivedNotificationHandlerTest {

    private OrderLookup orderLookup;
    private OperatorEmailLookup emailLookup;
    private EmailSender emailSender;
    private OrderReceivedNotificationHandler handler;

    @BeforeEach
    void setUp() {
        orderLookup = mock(OrderLookup.class);
        emailLookup = mock(OperatorEmailLookup.class);
        emailSender = mock(EmailSender.class);
        handler = new OrderReceivedNotificationHandler(
            new ObjectMapper(), orderLookup, emailLookup, emailSender);
    }

    @Test
    void handle_shouldSendEmailWithItemsWhenOrderAndEmailFound() {
        OrderInfo order = new OrderInfo("o-1", "op-1",
            List.of(new OrderInfo.OrderItem("SKU-A", 3)));
        when(orderLookup.findById("o-1")).thenReturn(Optional.of(order));
        when(emailLookup.findEmailByOperatorId("op-1")).thenReturn(Optional.of("op@example.com"));

        handler.handle("{\"orderId\":\"o-1\",\"lat\":4.7,\"lng\":-74.0,\"items\":[{\"sku\":\"SKU-A\",\"quantity\":3}]}");

        ArgumentCaptor<EmailNotification> captor = ArgumentCaptor.forClass(EmailNotification.class);
        verify(emailSender).send(captor.capture());

        EmailNotification email = captor.getValue();
        assertEquals("op@example.com", email.to());
        assertTrue(email.subject().contains("o-1"));
        assertTrue(email.bodyText().contains("SKU-A"));
        assertTrue(email.bodyHtml().contains("SKU-A"));
    }

    @Test
    void handle_shouldSkipWhenOrderNotFound() {
        when(orderLookup.findById("o-1")).thenReturn(Optional.empty());

        handler.handle("{\"orderId\":\"o-1\",\"lat\":4.7,\"lng\":-74.0,\"items\":[]}");

        verify(emailSender, never()).send(any());
    }

    @Test
    void handle_shouldSkipWhenEmailNotFound() {
        OrderInfo order = new OrderInfo("o-1", "op-1", List.of());
        when(orderLookup.findById("o-1")).thenReturn(Optional.of(order));
        when(emailLookup.findEmailByOperatorId("op-1")).thenReturn(Optional.empty());

        handler.handle("{\"orderId\":\"o-1\",\"lat\":4.7,\"lng\":-74.0,\"items\":[]}");

        verify(emailSender, never()).send(any());
    }

    @Test
    void eventType_shouldReturnOrderReceived() {
        assertEquals("OrderReceived", handler.eventType());
    }
}
