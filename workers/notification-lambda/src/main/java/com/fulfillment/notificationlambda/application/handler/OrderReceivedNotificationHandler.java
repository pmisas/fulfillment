package com.fulfillment.notificationlambda.application.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.notificationlambda.domain.model.EmailNotification;
import com.fulfillment.notificationlambda.domain.model.OrderInfo;
import com.fulfillment.notificationlambda.domain.ports.EmailSender;
import com.fulfillment.notificationlambda.domain.ports.OperatorEmailLookup;
import com.fulfillment.notificationlambda.domain.ports.OrderLookup;
import com.fulfillment.notificationlambda.infrastructure.messaging.dto.OrderReceivedPayload;

import java.util.Optional;
import java.util.stream.Collectors;

public class OrderReceivedNotificationHandler implements EventNotificationHandler {

    private final ObjectMapper mapper;
    private final OrderLookup orderLookup;
    private final OperatorEmailLookup emailLookup;
    private final EmailSender emailSender;

    public OrderReceivedNotificationHandler(
        ObjectMapper mapper,
        OrderLookup orderLookup,
        OperatorEmailLookup emailLookup,
        EmailSender emailSender
    ) {
        this.mapper = mapper;
        this.orderLookup = orderLookup;
        this.emailLookup = emailLookup;
        this.emailSender = emailSender;
    }

    @Override
    public String eventType() {
        return "OrderReceived";
    }

    @Override
    public void handle(String payload) {
        System.out.println("OrderReceived handler payload=" + payload);

        OrderReceivedPayload event = parse(payload);

        Optional<OrderInfo> orderOpt = orderLookup.findById(event.orderId());
        if (orderOpt.isEmpty()) {
            System.out.println("Order not found for orderId=" + event.orderId());
            return;
        }

        OrderInfo order = orderOpt.get();
        System.out.println("Order found orderId=" + order.orderId() + " operatorId=" + order.operatorId());

        Optional<String> emailOpt = emailLookup.findEmailByOperatorId(order.operatorId());
        if (emailOpt.isEmpty()) {
            System.out.println("Email not found for operatorId=" + order.operatorId() + " orderId=" + event.orderId());
            return;
        }

        String itemsText = order.items().stream()
            .map(i -> "  - " + i.sku() + " x" + i.quantity())
            .collect(Collectors.joining("\n"));

        String itemsHtml = order.items().stream()
            .map(i -> "<li><strong>" + escapeHtml(i.sku()) + "</strong> - cantidad: " + i.quantity() + "</li>")
            .collect(Collectors.joining("\n"));

        System.out.println("Sending email to=" + emailOpt.get());

        emailSender.send(new EmailNotification(
            emailOpt.get(),
            "Orden #" + event.orderId() + " recibida",
            buildText(event.orderId(), itemsText),
            buildHtml(event.orderId(), itemsHtml)
        ));

        System.out.println("OrderReceived notification sent for orderId=" + event.orderId());
    }

    private String buildText(String orderId, String items) {
        return "Tu orden #" + orderId + " ha sido recibida y esta siendo procesada.\n\n"
            + "Articulos:\n" + items + "\n\n"
            + "Te notificaremos cuando tu pedido este en camino.";
    }

    private String buildHtml(String orderId, String items) {
        return """
            <html>
              <head>
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
                <meta charset="UTF-8">
                <title>Orden recibida</title>
              </head>
              <body style="font-family: Arial, sans-serif; color: #222;">
                <h2>Tu orden ha sido recibida</h2>
                <p>Tu orden <strong>#%s</strong> ha sido recibida y esta siendo procesada.</p>
                <p><strong>Articulos:</strong></p>
                <ul>
                  %s
                </ul>
                <p>Te notificaremos cuando tu pedido este en camino.</p>
              </body>
            </html>
            """.formatted(escapeHtml(orderId), items);
    }

    private OrderReceivedPayload parse(String json) {
        try {
            return mapper.readValue(json, OrderReceivedPayload.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid OrderReceived payload: " + e.getMessage(), e);
        }
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
