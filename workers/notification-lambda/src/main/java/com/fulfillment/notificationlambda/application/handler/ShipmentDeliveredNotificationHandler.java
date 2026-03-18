package com.fulfillment.notificationlambda.application.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.notificationlambda.domain.model.EmailNotification;
import com.fulfillment.notificationlambda.domain.model.OrderInfo;
import com.fulfillment.notificationlambda.domain.ports.EmailSender;
import com.fulfillment.notificationlambda.domain.ports.OperatorEmailLookup;
import com.fulfillment.notificationlambda.domain.ports.OrderLookup;
import com.fulfillment.notificationlambda.infrastructure.messaging.dto.ShipmentDeliveredPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ShipmentDeliveredNotificationHandler implements EventNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(ShipmentDeliveredNotificationHandler.class);

    private final ObjectMapper mapper;
    private final OrderLookup orderLookup;
    private final OperatorEmailLookup emailLookup;
    private final EmailSender emailSender;

    public ShipmentDeliveredNotificationHandler(ObjectMapper mapper, OrderLookup orderLookup,
                                                OperatorEmailLookup emailLookup, EmailSender emailSender) {
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
        ShipmentDeliveredPayload event = parse(payload);

        Optional<OrderInfo> orderOpt = orderLookup.findById(event.orderId());
        if (orderOpt.isEmpty()) {
            log.warn("Order {} not found, skipping ShipmentDelivered notification", event.orderId());
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
            "Tu orden #" + event.orderId() + " ha sido entregada",
            buildText(event),
            buildHtml(event)
        ));

        log.info("ShipmentDelivered notification sent for order={}, shipment={}",
            event.orderId(), event.shipmentId());
    }

    private String buildText(ShipmentDeliveredPayload e) {
        return "¡Tu orden #" + e.orderId() + " ha sido entregada exitosamente!\n\n"
            + "ID de envío: " + e.shipmentId() + "\n\n"
            + "Gracias por confiar en nosotros.";
    }

    private String buildHtml(ShipmentDeliveredPayload e) {
        return """
            <h2>¡Tu orden ha sido entregada!</h2>
            <p>Tu orden <strong>#%s</strong> ha sido entregada exitosamente.</p>
            <ul>
              <li><strong>ID de envío:</strong> %s</li>
            </ul>
            <p>Gracias por confiar en nosotros.</p>
            """.formatted(e.orderId(), e.shipmentId());
    }

    private ShipmentDeliveredPayload parse(String json) {
        try {
            return mapper.readValue(json, ShipmentDeliveredPayload.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ShipmentDelivered payload: " + e.getMessage(), e);
        }
    }
}
