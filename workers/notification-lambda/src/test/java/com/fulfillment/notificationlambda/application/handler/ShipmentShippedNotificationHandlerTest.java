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

class ShipmentShippedNotificationHandlerTest {

    private OrderLookup orderLookup;
    private OperatorEmailLookup emailLookup;
    private EmailSender emailSender;
    private ShipmentShippedNotificationHandler handler;

    @BeforeEach
    void setUp() {
        orderLookup = mock(OrderLookup.class);
        emailLookup = mock(OperatorEmailLookup.class);
        emailSender = mock(EmailSender.class);
        handler = new ShipmentShippedNotificationHandler(
            new ObjectMapper(), orderLookup, emailLookup, emailSender);
    }

    @Test
    void handle_shouldSendEmailWithCarrierAndEta() {
        OrderInfo order = new OrderInfo("o-1", "op-1", List.of());
        when(orderLookup.findById("o-1")).thenReturn(Optional.of(order));
        when(emailLookup.findEmailByOperatorId("op-1")).thenReturn(Optional.of("op@example.com"));

        String payload = """
            {"orderId":"o-1","shipmentId":"sh-1","trackingId":"trk-1",
             "carrier":"FEDEX","estimatedDeliveryAt":"2026-03-25T12:00:00Z"}
            """;
        handler.handle(payload);

        ArgumentCaptor<EmailNotification> captor = ArgumentCaptor.forClass(EmailNotification.class);
        verify(emailSender).send(captor.capture());

        EmailNotification email = captor.getValue();
        assertEquals("op@example.com", email.to());
        assertTrue(email.subject().contains("o-1"));
        assertTrue(email.bodyText().contains("sh-1"));
        assertTrue(email.bodyText().contains("trk-1"));
        assertTrue(email.bodyText().contains("FEDEX"));
        assertTrue(email.bodyText().contains("2026-03-25"));
        assertTrue(email.bodyHtml().contains("FEDEX"));
    }

    @Test
    void handle_shouldSendEmailWithoutEtaWhenNull() {
        OrderInfo order = new OrderInfo("o-1", "op-1", List.of());
        when(orderLookup.findById("o-1")).thenReturn(Optional.of(order));
        when(emailLookup.findEmailByOperatorId("op-1")).thenReturn(Optional.of("op@example.com"));

        String payload = """
            {"orderId":"o-1","shipmentId":"sh-1","trackingId":"trk-1","carrier":"DHL"}
            """;
        handler.handle(payload);

        ArgumentCaptor<EmailNotification> captor = ArgumentCaptor.forClass(EmailNotification.class);
        verify(emailSender).send(captor.capture());

        assertFalse(captor.getValue().bodyText().contains("estimada"));
    }

    @Test
    void handle_shouldSkipWhenOrderNotFound() {
        when(orderLookup.findById("o-1")).thenReturn(Optional.empty());

        handler.handle("{\"orderId\":\"o-1\",\"shipmentId\":\"sh-1\",\"trackingId\":\"trk-1\"}");

        verify(emailSender, never()).send(any());
    }

    @Test
    void eventType_shouldReturnShipmentShipped() {
        assertEquals("ShipmentShipped", handler.eventType());
    }
}
