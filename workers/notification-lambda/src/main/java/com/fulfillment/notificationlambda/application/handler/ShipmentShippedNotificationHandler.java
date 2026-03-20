package com.fulfillment.notificationlambda.application.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.notificationlambda.domain.model.EmailNotification;
import com.fulfillment.notificationlambda.domain.model.OrderInfo;
import com.fulfillment.notificationlambda.domain.ports.EmailSender;
import com.fulfillment.notificationlambda.domain.ports.OperatorEmailLookup;
import com.fulfillment.notificationlambda.domain.ports.OrderLookup;
import com.fulfillment.notificationlambda.infrastructure.messaging.dto.ShipmentShippedPayload;

import java.util.Optional;

public class ShipmentShippedNotificationHandler implements EventNotificationHandler {

    private final ObjectMapper mapper;
    private final OrderLookup orderLookup;
    private final OperatorEmailLookup emailLookup;
    private final EmailSender emailSender;

    public ShipmentShippedNotificationHandler(
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
        return "ShipmentShipped";
    }

    @Override
    public void handle(String payload) {
        System.out.println("ShipmentShipped handler payload=" + payload);

        ShipmentShippedPayload event = parse(payload);

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

        System.out.println("Sending shipment shipped email to=" + emailOpt.get());

        emailSender.send(new EmailNotification(
            emailOpt.get(),
            "Tu orden #" + event.orderId() + " esta en camino",
            buildText(event),
            buildHtml(event)
        ));

        System.out.println("ShipmentShipped notification sent for orderId=" + event.orderId()
            + " shipmentId=" + event.shipmentId());
    }

    private String buildText(ShipmentShippedPayload e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tu orden #").append(e.orderId()).append(" esta en camino.\n\n");
        sb.append("ID de envio: ").append(e.shipmentId()).append("\n");
        sb.append("Numero de guia: ").append(e.trackingId()).append("\n");
        if (e.carrier() != null && !e.carrier().isBlank()) {
            sb.append("Transportadora: ").append(e.carrier()).append("\n");
        }
        if (e.estimatedDeliveryAt() != null && !e.estimatedDeliveryAt().isBlank()) {
            sb.append("Fecha estimada de entrega: ").append(e.estimatedDeliveryAt()).append("\n");
        }
        sb.append("\nPuedes rastrear tu envio con el numero de guia.");
        return sb.toString();
    }

    private String buildHtml(ShipmentShippedPayload e) {
        String carrierRow = (e.carrier() != null && !e.carrier().isBlank())
            ? "<li><strong>Transportadora:</strong> " + escapeHtml(e.carrier()) + "</li>" : "";

        String etaRow = (e.estimatedDeliveryAt() != null && !e.estimatedDeliveryAt().isBlank())
            ? "<li><strong>Fecha estimada de entrega:</strong> " + escapeHtml(e.estimatedDeliveryAt()) + "</li>" : "";

        return """
            <html>
              <head>
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
                <meta charset="UTF-8">
                <title>Envio despachado</title>
              </head>
              <body style="font-family: Arial, sans-serif; color: #222;">
                <h2>Tu orden esta en camino</h2>
                <p>Tu orden <strong>#%s</strong> ha sido despachada.</p>
                <ul>
                  <li><strong>ID de envio:</strong> %s</li>
                  <li><strong>Numero de guia:</strong> %s</li>
                  %s
                  %s
                </ul>
                <p>Puedes rastrear tu envio usando el numero de guia.</p>
              </body>
            </html>
            """.formatted(
                escapeHtml(e.orderId()),
                escapeHtml(e.shipmentId()),
                escapeHtml(e.trackingId()),
                carrierRow,
                etaRow
            );
    }

    private ShipmentShippedPayload parse(String json) {
        try {
            return mapper.readValue(json, ShipmentShippedPayload.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ShipmentShipped payload: " + e.getMessage(), e);
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
