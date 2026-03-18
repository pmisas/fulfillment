package com.fulfillment.notificationlambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.notificationlambda.application.NotificationDispatcher;
import com.fulfillment.notificationlambda.infrastructure.config.EnvConfig;
import com.fulfillment.notificationlambda.infrastructure.email.SesEmailSender;
import com.fulfillment.notificationlambda.infrastructure.operator.CognitoOperatorEmailLookup;
import com.fulfillment.notificationlambda.infrastructure.order.DynamoOrderLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sesv2.SesV2Client;

import java.util.ArrayList;
import java.util.List;

public class NotificationHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private static final Logger log = LoggerFactory.getLogger(NotificationHandler.class);

    private final NotificationDispatcher dispatcher;

    public NotificationHandler() {
        EnvConfig config = EnvConfig.fromEnvironment();
        Region region = Region.of(config.awsRegion());

        ObjectMapper mapper = new ObjectMapper()
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

    NotificationHandler(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public SQSBatchResponse handleRequest(SQSEvent event, Context context) {
        List<SQSBatchResponse.BatchItemFailure> failures = new ArrayList<>();

        for (SQSEvent.SQSMessage message : event.getRecords()) {
            try {
                String eventType = extractEventType(message);
                if (eventType == null || eventType.isBlank()) {
                    log.warn("Message {} has no eventType attribute, skipping", message.getMessageId());
                    continue;
                }
                log.info("Processing message={} eventType={}", message.getMessageId(), eventType);
                dispatcher.dispatch(eventType, message.getBody());
            } catch (Exception e) {
                log.error("Failed to process message {}: {}", message.getMessageId(), e.getMessage(), e);
                failures.add(SQSBatchResponse.BatchItemFailure.builder()
                    .withItemIdentifier(message.getMessageId())
                    .build());
            }
        }

        return SQSBatchResponse.builder().withBatchItemFailures(failures).build();
    }

    private String extractEventType(SQSEvent.SQSMessage message) {
        if (message.getMessageAttributes() == null) return null;
        SQSEvent.MessageAttribute attr = message.getMessageAttributes().get("eventType");
        return attr != null ? attr.getStringValue() : null;
    }
}
