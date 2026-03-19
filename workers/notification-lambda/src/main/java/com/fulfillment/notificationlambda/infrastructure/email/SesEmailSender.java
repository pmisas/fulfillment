package com.fulfillment.notificationlambda.infrastructure.email;

import com.fulfillment.notificationlambda.domain.model.EmailNotification;
import com.fulfillment.notificationlambda.domain.ports.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

public class SesEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SesEmailSender.class);

    private final SesV2Client ses;
    private final String fromEmail;

    public SesEmailSender(SesV2Client ses, String fromEmail) {
        this.ses = ses;
        this.fromEmail = fromEmail;
    }

    @Override
    public void send(EmailNotification notification) {
        SendEmailRequest request = SendEmailRequest.builder()
            .fromEmailAddress(fromEmail)
            .destination(Destination.builder()
                .toAddresses(notification.to())
                .build())
            .content(EmailContent.builder()
                .simple(Message.builder()
                    .subject(Content.builder()
                        .data(notification.subject())
                        .charset("UTF-8")
                        .build())
                    .body(Body.builder()
                        .text(Content.builder()
                            .data(notification.bodyText())
                            .charset("UTF-8")
                            .build())
                        .html(Content.builder()
                            .data(notification.bodyHtml())
                            .charset("UTF-8")
                            .build())
                        .build())
                    .build())
                .build())
            .build();

        ses.sendEmail(request);
        log.info("Email sent to={} subject='{}'", notification.to(), notification.subject());
    }
}
