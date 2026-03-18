package com.fulfillment.notificationlambda.domain.ports;

import com.fulfillment.notificationlambda.domain.model.EmailNotification;

public interface EmailSender {
    void send(EmailNotification notification);
}
