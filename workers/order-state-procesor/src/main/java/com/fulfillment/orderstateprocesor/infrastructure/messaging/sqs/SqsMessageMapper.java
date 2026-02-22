package com.fulfillment.orderstateprocesor.infrastructure.messaging.sqs;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fulfillment.orderstateprocesor.application.dto.ProcessEventCommand;

import software.amazon.awssdk.services.sqs.model.Message;

@Component
public class SqsMessageMapper {

    public ProcessEventCommand toCommand(Message msg) {
        Map<String, software.amazon.awssdk.services.sqs.model.MessageAttributeValue> attrs = msg.messageAttributes();

        String eventId = getAttr(attrs, "eventId");
        String eventType = getAttr(attrs, "eventType");

        if (eventId == null || eventId.isBlank()) {
            eventId = msg.messageId(); 
        }
        if (eventType == null || eventType.isBlank()) {
            eventType = "UNKNOWN";
        }

        return new ProcessEventCommand(eventId, eventType, msg.body());
    }

    private String getAttr(Map<String, software.amazon.awssdk.services.sqs.model.MessageAttributeValue> attrs, String key) {
        if (attrs == null) return null;
        var v = attrs.get(key);
        if (v == null) return null;
        return v.stringValue();
    }
}
