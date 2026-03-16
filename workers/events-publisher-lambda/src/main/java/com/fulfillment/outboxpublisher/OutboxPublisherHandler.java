package com.fulfillment.outboxpublisher;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.time.Instant;
import java.util.*;

public class OutboxPublisherHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final String tableName;
    private final String gsiName;
    private final String queueUrl;
    private final int maxBatch;

    private final DynamoDbClient dynamo;
    private final SqsClient sqs;

    public OutboxPublisherHandler() {
        this.tableName = env("OUTBOX_TABLE", "OutboxEvents");
        this.gsiName   = env("OUTBOX_GSI", "ByPublishStatus");
        this.queueUrl  = env("SQS_QUEUE_URL", null);
        this.maxBatch  = Integer.parseInt(env("MAX_BATCH", "25"));
        String regionStr = env("AWS_REGION", "us-east-1");

        if (this.queueUrl == null || this.queueUrl.isBlank()) {
            throw new IllegalStateException("Missing env var SQS_QUEUE_URL");
        }

        Region region = Region.of(regionStr);
        this.dynamo = DynamoDbClient.builder().region(region).build();
        this.sqs = SqsClient.builder().region(region).build();
    }

    OutboxPublisherHandler(String tableName, String gsiName, String queueUrl, int maxBatch,
                           DynamoDbClient dynamo, SqsClient sqs) {
        this.tableName = tableName;
        this.gsiName   = gsiName;
        this.queueUrl  = queueUrl;
        this.maxBatch  = maxBatch;
        this.dynamo    = dynamo;
        this.sqs       = sqs;
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        String runId = UUID.randomUUID().toString();
        int published = 0;
        int skipped = 0;
        int failed = 0;

        context.getLogger().log("OutboxPublisher start runId=" + runId + " maxBatch=" + maxBatch + "\n");

        List<Map<String, AttributeValue>> pendingEvents = queryPending(maxBatch);

        context.getLogger().log("Found pending events=" + pendingEvents.size() + "\n");

        for (Map<String, AttributeValue> item : pendingEvents) {
            String eventId = getS(item, "eventId");
            String eventType = getS(item, "eventType");
            String aggregateId = getS(item, "aggregateId");
            String payload = getS(item, "payload");

            if (eventId == null || payload == null) {
                skipped++;
                context.getLogger().log("Skipping invalid item (missing eventId/payload)\n");
                continue;
            }

            try {
                sendToSqs(eventId, eventType, aggregateId, payload);

                boolean updated = markAsSent(eventId);

                if (updated) {
                    published++;
                } else {
                    skipped++;
                }
            } catch (Exception ex) {
                failed++;
                context.getLogger().log("Failed publish eventId=" + eventId + " err=" + ex.getMessage() + "\n");

                safeMarkFailed(eventId, ex.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("runId", runId);
        result.put("found", pendingEvents.size());
        result.put("published", published);
        result.put("skipped", skipped);
        result.put("failed", failed);

        context.getLogger().log("OutboxPublisher done: " + result + "\n");
        return result;
    }

    private List<Map<String, AttributeValue>> queryPending(int limit) {
        Map<String, String> names = Map.of("#ps", "publishStatus");
        Map<String, AttributeValue> values = Map.of(":pending", AttributeValue.builder().s("PENDING").build());

        QueryRequest request = QueryRequest.builder()
                .tableName(tableName)   
                .indexName(gsiName)
                .keyConditionExpression("#ps = :pending")
                .expressionAttributeNames(names)
                .expressionAttributeValues(values)
                .scanIndexForward(true)
                .limit(limit)
                .build();

        QueryResponse resp = dynamo.query(request);
        return resp.items() == null ? List.of() : resp.items();
    }

    private void sendToSqs(String eventId, String eventType, String aggregateId, String payload) {
        Map<String, MessageAttributeValue> attrs = new HashMap<>();
        if (eventType != null) {
            attrs.put("eventType", MessageAttributeValue.builder()
                    .dataType("String").stringValue(eventType).build());
        }
        if (aggregateId != null) {
            attrs.put("aggregateId", MessageAttributeValue.builder()
                    .dataType("String").stringValue(aggregateId).build());
        }
        attrs.put("eventId", MessageAttributeValue.builder()
                .dataType("String").stringValue(eventId).build());

        SendMessageRequest req = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(payload)
                .messageAttributes(attrs)
                .build();

        sqs.sendMessage(req);
    }

    private boolean markAsSent(String eventId) {
        long nowMs = Instant.now().toEpochMilli();

        Map<String, AttributeValue> key = Map.of(
                "eventId", AttributeValue.builder().s(eventId).build()
        );

        Map<String, String> names = Map.of(
                "#ps", "publishStatus",
                "#pa", "publishedAt",
                "#att", "attempts"
        );

        Map<String, AttributeValue> values = Map.of(
                ":sent", AttributeValue.builder().s("SENT").build(),
                ":pending", AttributeValue.builder().s("PENDING").build(),
                ":now", AttributeValue.builder().n(Long.toString(nowMs)).build(),
                ":one", AttributeValue.builder().n("1").build(),
                ":zero", AttributeValue.builder().n("0").build()
        );

        UpdateItemRequest req = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .updateExpression("SET #ps = :sent, #pa = :now, #att = if_not_exists(#att, :zero) + :one")
                .conditionExpression("#ps = :pending")
                .expressionAttributeNames(names)
                .expressionAttributeValues(values)
                .returnValues(ReturnValue.NONE)
                .build();

        try {
            dynamo.updateItem(req);
            return true;
        } catch (ConditionalCheckFailedException ccfe) {
            return false;
        }
    }

    private void safeMarkFailed(String eventId, String error) {
        try {
            Map<String, AttributeValue> key = Map.of(
                    "eventId", AttributeValue.builder().s(eventId).build()
            );

            Map<String, String> names = Map.of(
                    "#ps", "publishStatus",
                    "#le", "lastError",
                    "#att", "attempts"
            );

            String safeError = error == null ? "unknown" : error;
            if (safeError.length() > 500) safeError = safeError.substring(0, 500);

            Map<String, AttributeValue> values = Map.of(
                    ":failed", AttributeValue.builder().s("FAILED").build(),
                    ":err", AttributeValue.builder().s(safeError).build(),
                    ":one", AttributeValue.builder().n("1").build(),
                    ":zero", AttributeValue.builder().n("0").build()
            );

            UpdateItemRequest req = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .updateExpression("SET #ps = :failed, #le = :err, #att = if_not_exists(#att, :zero) + :one")
                    .expressionAttributeNames(names)
                    .expressionAttributeValues(values)
                    .build();

            dynamo.updateItem(req);
        } catch (Exception ignored) {
        }
    }

    private static String getS(Map<String, AttributeValue> item, String attr) {
        AttributeValue v = item.get(attr);
        return (v == null || v.s() == null) ? null : v.s();
    }

    private static String env(String key, String defaultValue) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) return defaultValue;
        return v;
    }
}