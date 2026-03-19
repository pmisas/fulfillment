package com.fulfillment.orderstateprocesor.infrastructure.messaging.sqs;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.orderstateprocesor.application.dto.ProcessEventCommand;

import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

@Component
public class SqsMessageMapper {

    private final ObjectMapper objectMapper;

    public SqsMessageMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ProcessEventCommand toCommand(Message msg) {
        try {
            Map<String, MessageAttributeValue> attrs = msg.messageAttributes();

            String eventId = getAttr(attrs, "eventId");
            String eventType = getAttr(attrs, "eventType");

            if (eventId == null || eventId.isBlank()) {
                eventId = msg.messageId();
            }

            if (eventType != null && !eventType.isBlank()) {
                return new ProcessEventCommand(eventId, eventType, msg.body());
            }

            JsonNode root = objectMapper.readTree(msg.body());

            boolean looksLikeSnsEnvelope = root.has("Type") && root.has("Message");
            if (looksLikeSnsEnvelope) {
                String snsEventType = root.path("MessageAttributes")
                    .path("eventType")
                    .path("Value")
                    .asText(null);

                String snsEventId = root.path("MessageAttributes")
                    .path("eventId")
                    .path("Value")
                    .asText(null);

                String payload = root.path("Message").asText(null);

                if (snsEventId == null || snsEventId.isBlank()) {
                    snsEventId = eventId;
                }

                if (snsEventType == null || snsEventType.isBlank()) {
                    snsEventType = "UNKNOWN";
                }

                if (payload == null || payload.isBlank()) {
                    payload = msg.body();
                }

                return new ProcessEventCommand(snsEventId, snsEventType, payload);
            }

            if (root.has("eventType") && root.has("payload")) {
                String directEventType = root.path("eventType").asText("UNKNOWN");
                String directEventId = root.path("eventId").asText(eventId);
                String directPayload = objectMapper.writeValueAsString(root.path("payload"));

                return new ProcessEventCommand(directEventId, directEventType, directPayload);
            }

            return new ProcessEventCommand(eventId, "UNKNOWN", msg.body());

        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Could not map SQS messageId=" + msg.messageId() + ": " + e.getMessage(), e
            );
        }
    }

    private String getAttr(Map<String, MessageAttributeValue> attrs, String key) {
        if (attrs == null) return null;
        MessageAttributeValue v = attrs.get(key);
        if (v == null) return null;
        return v.stringValue();
    }
}
