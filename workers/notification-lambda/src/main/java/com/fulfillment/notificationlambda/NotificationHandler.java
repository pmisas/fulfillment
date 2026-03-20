package com.fulfillment.notificationlambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.notificationlambda.application.NotificationDispatcher;
import com.fulfillment.notificationlambda.infrastructure.config.EnvConfig;
import com.fulfillment.notificationlambda.infrastructure.email.SesEmailSender;
import com.fulfillment.notificationlambda.infrastructure.operator.CognitoOperatorEmailLookup;
import com.fulfillment.notificationlambda.infrastructure.order.DynamoOrderLookup;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sesv2.SesV2Client;

import java.util.ArrayList;
import java.util.List;

public class NotificationHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private final NotificationDispatcher dispatcher;
    private final ObjectMapper mapper;

    public NotificationHandler() {
        EnvConfig config = EnvConfig.fromEnvironment();
        Region region = Region.of(config.awsRegion());

        this.mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        DynamoDbClient dynamo = DynamoDbClient.builder().region(region).build();
        CognitoIdentityProviderClient cognito = CognitoIdentityProviderClient.builder().region(region).build();
        SesV2Client ses = SesV2Client.builder().region(region).build();

        this.dispatcher = new NotificationDispatcher(
            mapper,
            new DynamoOrderLookup(dynamo, config.ordersTable()),
            new CognitoOperatorEmailLookup(cognito, config.cognitoUserPoolId()),
            new SesEmailSender(ses, config.sesFromEmail())
        );
    }

    @Override
    public SQSBatchResponse handleRequest(SQSEvent event, Context context) {
        List<SQSBatchResponse.BatchItemFailure> failures = new ArrayList<>();

        for (SQSEvent.SQSMessage message : event.getRecords()) {
            try {
                ParsedMessage parsed = parseMessage(message);

                context.getLogger().log(
                    "Processing messageId=" + message.getMessageId()
                        + " eventType=" + parsed.eventType()
                        + " payload=" + parsed.payload() + "\n"
                );

                dispatcher.dispatch(parsed.eventType(), parsed.payload());

            } catch (Exception e) {
                context.getLogger().log(
                    "Failed processing messageId=" + message.getMessageId()
                        + " error=" + e.getMessage() + "\n"
                );

                failures.add(SQSBatchResponse.BatchItemFailure.builder()
                    .withItemIdentifier(message.getMessageId())
                    .build());
            }
        }

        return SQSBatchResponse.builder()
            .withBatchItemFailures(failures)
            .build();
    }

    private ParsedMessage parseMessage(SQSEvent.SQSMessage message) throws Exception {
        String sqsEventType = extractSqsAttribute(message, "eventType");
        String sqsEventId = extractSqsAttribute(message, "eventId");

        if (sqsEventType != null && !sqsEventType.isBlank()) {
            return new ParsedMessage(
                sqsEventId != null ? sqsEventId : message.getMessageId(),
                sqsEventType,
                message.getBody()
            );
        }

        JsonNode root = mapper.readTree(message.getBody());

        boolean looksLikeSnsEnvelope = root.has("Type") && root.has("Message");
        if (looksLikeSnsEnvelope) {
            String eventType = root.path("MessageAttributes")
                .path("eventType")
                .path("Value")
                .asText(null);

            String eventId = root.path("MessageAttributes")
                .path("eventId")
                .path("Value")
                .asText(message.getMessageId());

            String payload = root.path("Message").asText(null);

            if (eventType == null || eventType.isBlank()) {
                throw new IllegalArgumentException("SNS envelope without eventType");
            }

            if (payload == null || payload.isBlank()) {
                throw new IllegalArgumentException("SNS envelope without Message payload");
            }

            return new ParsedMessage(eventId, eventType, payload);
        }

        if (root.has("eventType") && root.has("payload")) {
            String eventType = root.path("eventType").asText(null);
            String eventId = root.path("eventId").asText(message.getMessageId());
            String payload = mapper.writeValueAsString(root.path("payload"));

            if (eventType == null || eventType.isBlank()) {
                throw new IllegalArgumentException("Direct message without eventType");
            }

            return new ParsedMessage(eventId, eventType, payload);
        }

        throw new IllegalArgumentException("Unknown message format");
    }

    private String extractSqsAttribute(SQSEvent.SQSMessage message, String key) {
        if (message.getMessageAttributes() == null) return null;
        SQSEvent.MessageAttribute attr = message.getMessageAttributes().get(key);
        return attr != null ? attr.getStringValue() : null;
    }

    private record ParsedMessage(String eventId, String eventType, String payload) {}
}
