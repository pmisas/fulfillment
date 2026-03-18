package com.fulfillment.notificationlambda.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.notificationlambda.application.handler.EventNotificationHandler;
import com.fulfillment.notificationlambda.application.handler.OrderReceivedNotificationHandler;
import com.fulfillment.notificationlambda.application.handler.ShipmentDeliveredNotificationHandler;
import com.fulfillment.notificationlambda.application.handler.ShipmentShippedNotificationHandler;
import com.fulfillment.notificationlambda.domain.ports.EmailSender;
import com.fulfillment.notificationlambda.domain.ports.OperatorEmailLookup;
import com.fulfillment.notificationlambda.domain.ports.OrderLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final Map<String, EventNotificationHandler> handlers;

    public NotificationDispatcher(ObjectMapper mapper, OrderLookup orderLookup,
                                  OperatorEmailLookup emailLookup, EmailSender emailSender) {
        List<EventNotificationHandler> handlerList = List.of(
            new OrderReceivedNotificationHandler(mapper, orderLookup, emailLookup, emailSender),
            new ShipmentShippedNotificationHandler(mapper, orderLookup, emailLookup, emailSender),
            new ShipmentDeliveredNotificationHandler(mapper, orderLookup, emailLookup, emailSender)
        );
        this.handlers = handlerList.stream()
            .collect(Collectors.toMap(EventNotificationHandler::eventType, h -> h));
    }

    NotificationDispatcher(Map<String, EventNotificationHandler> handlers) {
        this.handlers = handlers;
    }

    public void dispatch(String eventType, String payload) {
        if (eventType == null) {
            log.debug("Received message with null eventType, skipping");
            return;
        }
        EventNotificationHandler handler = handlers.get(eventType);
        if (handler == null) {
            log.debug("No notification handler registered for eventType={}, skipping", eventType);
            return;
        }
        log.info("Dispatching notification for eventType={}", eventType);
        handler.handle(payload);
    }
}
