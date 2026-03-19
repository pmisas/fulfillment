package com.fulfillment.notificationlambda.domain.model;

public record EmailNotification(
    String to,
    String subject,
    String bodyText,
    String bodyHtml) {}
