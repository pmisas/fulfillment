package com.fulfillment.notificationlambda.application.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.notificationlambda.domain.model.EmailNotification;
import com.fulfillment.notificationlambda.domain.model.OrderInfo;
import com.fulfillment.notificationlambda.domain.ports.EmailSender;
import com.fulfillment.notificationlambda.domain.ports.OperatorEmailLookup;
import com.fulfillment.notificationlambda.domain.ports.OrderLookup;
import com.fulfillment.notificationlambda.infrastructure.messaging.dto.ShipmentShippedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ShipmentShippedNotificationHandler implements EventNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(ShipmentShippedNotificationHandler.class);

    private final ObjectMapper mapper;
    private final OrderLookup orderLookup;
    private final OperatorEmailLookup emailLookup;
    private final EmailSender emailSender;

    public ShipmentShippedNotificationHandler(ObjectMapper mapper, OrderLookup orderLookup,
                                              OperatorEmailLookup emailLookup, EmailSender emailSender) {
        this.mapper = mapper;
        this.orderLookup = orderLookup;
        this.emailLookup = emailLookup;
        this.emailSender = emailSender;
    }

    @Override
    public String eventType() {
        return "ShipmentShipped";
    }

    @Override
    public void handle(String payload) {
        ShipmentShippedPayload event = parse(payload);

        Optional<OrderInfo> orderOpt = orderLookup.findById(event.orderId());
        if (orderOpt.isEmpty()) {
            log.warn("Order {} not found, skipping ShipmentShipped notification", event.orderId());
            return;
        }

        OrderInfo order = orderOpt.get();
        Optional<String> emailOpt = emailLookup.findEmailByOperatorId(order.operatorId());
        if (emailOpt.isEmpty()) {
            log.warn("Email not found for operator {} (order {}), skipping notification",
                order.operatorId(), event.orderId());
            return;
        }

        emailSender.send(new EmailNotification(
            emailOpt.get(),
            "Tu orden #" + event.orderId() + " está en camino",
            buildText(event),
            buildHtml(event)
        ));

        log.info("ShipmentShipped notification sent for order={}, shipment={}",
            event.orderId(), event.shipmentId());
    }

    private String buildText(ShipmentShippedPayload e) {
        StringBuilder sb = new StringBuilder();
        sb.append("¡Tu orden #").append(e.orderId()).append(" está en camino!\n\n");
        sb.append("ID de envío: ").append(e.shipmentId()).append("\n");
        sb.append("Número de guía: ").append(e.trackingId()).append("\n");
        if (e.carrier() != null) sb.append("Transportadora: ").append(e.carrier()).append("\n");
        if (e.estimatedDeliveryAt() != null)
            sb.append("Fecha estimada de entrega: ").append(e.estimatedDeliveryAt()).append("\n");
        sb.append("\nPuedes rastrear tu envío con el número de guía a través de tu transportadora.");
        return sb.toString();
    }

    private String buildHtml(ShipmentShippedPayload e) {
        String carrierRow = e.carrier() != null
            ? "<li><strong>Transportadora:</strong> " + e.carrier() + "</li>" : "";
        String etaRow = e.estimatedDeliveryAt() != null
            ? "<li><strong>Fecha estimada de entrega:</strong> " + e.estimatedDeliveryAt() + "</li>" : "";
        return """
            <h2>¡Tu orden está en camino!</h2>
            <p>Tu orden <strong>#%s</strong> ha sido despachada.</p>
            <ul>
              <li><strong>ID de envío:</strong> %s</li>
              <li><strong>Número de guía:</strong> %s</li>
              %s
              %s
            </ul>
            <p>Puedes rastrear tu envío usando el número de guía con tu transportadora.</p>
            """.formatted(e.orderId(), e.shipmentId(), e.trackingId(), carrierRow, etaRow);
    }

    private ShipmentShippedPayload parse(String json) {
        try {
            return mapper.readValue(json, ShipmentShippedPayload.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ShipmentShipped payload: " + e.getMessage(), e);
        }
    }
}
