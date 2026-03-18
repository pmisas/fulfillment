package com.fulfillment.notificationlambda.application.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.notificationlambda.domain.model.EmailNotification;
import com.fulfillment.notificationlambda.domain.model.OrderInfo;
import com.fulfillment.notificationlambda.domain.ports.EmailSender;
import com.fulfillment.notificationlambda.domain.ports.OperatorEmailLookup;
import com.fulfillment.notificationlambda.domain.ports.OrderLookup;
import com.fulfillment.notificationlambda.infrastructure.messaging.dto.OrderReceivedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.stream.Collectors;

public class OrderReceivedNotificationHandler implements EventNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderReceivedNotificationHandler.class);

    private final ObjectMapper mapper;
    private final OrderLookup orderLookup;
    private final OperatorEmailLookup emailLookup;
    private final EmailSender emailSender;

    public OrderReceivedNotificationHandler(
                ObjectMapper mapper, OrderLookup orderLookup,
                OperatorEmailLookup emailLookup, EmailSender emailSender) {
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
        OrderReceivedPayload event = parse(payload);

        Optional<OrderInfo> orderOpt = orderLookup.findById(event.orderId());
        if (orderOpt.isEmpty()) {
            log.warn("Order {} not found, skipping OrderReceived notification", event.orderId());
            return;
        }

        OrderInfo order = orderOpt.get();
        Optional<String> emailOpt = emailLookup.findEmailByOperatorId(order.operatorId());
        if (emailOpt.isEmpty()) {
            log.warn("Email not found for operator {} (order {}), skipping notification",
                order.operatorId(), event.orderId());
            return;
        }

        String itemsText = order.items().stream()
            .map(i -> "  - " + i.sku() + " x" + i.quantity())
            .collect(Collectors.joining("\n"));
        String itemsHtml = order.items().stream()
            .map(i -> "<li><strong>" + i.sku() + "</strong> — cantidad: " + i.quantity() + "</li>")
            .collect(Collectors.joining("\n"));

        emailSender.send(new EmailNotification(
            emailOpt.get(),
            "Orden #" + event.orderId() + " recibida",
            buildText(event.orderId(), itemsText),
            buildHtml(event.orderId(), itemsHtml)
        ));

        log.info("OrderReceived notification sent for order={}", event.orderId());
    }

    private String buildText(String orderId, String items) {
        return "Tu orden #" + orderId + " ha sido recibida y está siendo procesada.\n\n"
            + "Artículos:\n" + items + "\n\n"
            + "Te notificaremos cuando tu pedido esté en camino.";
    }

    private String buildHtml(String orderId, String items) {
        return """
            <h2>¡Tu orden ha sido recibida!</h2>
            <p>Tu orden <strong>#%s</strong> ha sido recibida y está siendo procesada.</p>
            <p><strong>Artículos:</strong></p>
            <ul>
            %s
            </ul>
            <p>Te notificaremos cuando tu pedido esté en camino.</p>
            """.formatted(orderId, items);
    }

    private OrderReceivedPayload parse(String json) {
        try {
            return mapper.readValue(json, OrderReceivedPayload.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid OrderReceived payload: " + e.getMessage(), e);
        }
    }
}
