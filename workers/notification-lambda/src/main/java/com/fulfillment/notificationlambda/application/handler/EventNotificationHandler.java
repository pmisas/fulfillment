package com.fulfillment.notificationlambda.application.handler;

public interface EventNotificationHandler {
    String eventType();
    void handle(String payload);
}
