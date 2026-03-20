package com.fulfillment.notificationlambda.application.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.notificationlambda.domain.model.EmailNotification;
import com.fulfillment.notificationlambda.domain.model.OrderInfo;
import com.fulfillment.notificationlambda.domain.ports.EmailSender;
import com.fulfillment.notificationlambda.domain.ports.OperatorEmailLookup;
import com.fulfillment.notificationlambda.domain.ports.OrderLookup;
import com.fulfillment.notificationlambda.infrastructure.messaging.dto.ShipmentDeliveredPayload;

import java.util.Optional;

public class ShipmentDeliveredNotificationHandler implements EventNotificationHandler {

    private final ObjectMapper mapper;
    private final OrderLookup orderLookup;
    private final OperatorEmailLookup emailLookup;
    private final EmailSender emailSender;

    public ShipmentDeliveredNotificationHandler(
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
        return "ShipmentDelivered";
    }

    @Override
    public void handle(String payload) {
        System.out.println("ShipmentDelivered handler payload=" + payload);

        ShipmentDeliveredPayload event = parse(payload);

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

        System.out.println("Sending shipment delivered email to=" + emailOpt.get());

        emailSender.send(new EmailNotification(
            emailOpt.get(),
            "Tu orden #" + event.orderId() + " ha sido entregada",
            buildText(event),
            buildHtml(event)
        ));

        System.out.println("ShipmentDelivered notification sent for orderId=" + event.orderId()
            + " shipmentId=" + event.shipmentId());
    }

    private String buildText(ShipmentDeliveredPayload e) {
        return "Tu orden #" + e.orderId() + " ha sido entregada exitosamente.\n\n"
            + "ID de envio: " + e.shipmentId() + "\n\n"
            + "Gracias por confiar en nosotros.";
    }

    private String buildHtml(ShipmentDeliveredPayload e) {
        return """
            <html>
              <head>
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
                <meta charset="UTF-8">
                <title>Envio entregado</title>
              </head>
              <body style="font-family: Arial, sans-serif; color: #222;">
                <h2>Tu orden ha sido entregada</h2>
                <p>Tu orden <strong>#%s</strong> ha sido entregada exitosamente.</p>
                <ul>
                  <li><strong>ID de envio:</strong> %s</li>
                </ul>
                <p>Gracias por confiar en nosotros.</p>
              </body>
            </html>
            """.formatted(
                escapeHtml(e.orderId()),
                escapeHtml(e.shipmentId())
            );
    }

    private ShipmentDeliveredPayload parse(String json) {
        try {
            return mapper.readValue(json, ShipmentDeliveredPayload.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ShipmentDelivered payload: " + e.getMessage(), e);
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
