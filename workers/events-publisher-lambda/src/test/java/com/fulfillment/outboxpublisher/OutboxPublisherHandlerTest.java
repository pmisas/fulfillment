package com.fulfillment.outboxpublisher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

class OutboxPublisherHandlerTest {

    private DynamoDbClient dynamo;
    private SqsClient sqs;
    private Context context;
    private LambdaLogger logger;
    private OutboxPublisherHandler handler;

    @BeforeEach
    void setUp() {
        dynamo  = mock(DynamoDbClient.class);
        sqs     = mock(SqsClient.class);
        context = mock(Context.class);
        logger  = mock(LambdaLogger.class);

        when(context.getLogger()).thenReturn(logger);

        handler = new OutboxPublisherHandler(
            "OutboxEvents",
            "ByPublishStatus",
            "https://sqs.us-east-1.amazonaws.com/123456/OrderEventsQueue",
            25,
            dynamo,
            sqs
        );
    }

    @Test
    void handleRequest_shouldPublishPendingEventsToSqsAndMarkAsSent() {
        Map<String, AttributeValue> item = pendingItem("evt-1", "OrderReceived", "order-1", "{\"orderId\":\"order-1\"}");

        QueryResponse queryResponse = QueryResponse.builder()
            .items(List.of(item))
            .build();

        when(dynamo.query(any(QueryRequest.class))).thenReturn(queryResponse);
        when(dynamo.updateItem(any(UpdateItemRequest.class))).thenReturn(UpdateItemResponse.builder().build());

        Map<String, Object> result = handler.handleRequest(Map.of(), context);

        assertEquals(1, result.get("found"));
        assertEquals(1, result.get("published"));
        assertEquals(0, result.get("skipped"));
        assertEquals(0, result.get("failed"));

        verify(sqs).sendMessage(any(SendMessageRequest.class));
        verify(dynamo).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void handleRequest_shouldReturnZeroCountsWhenNoPendingEvents() {
        QueryResponse emptyResponse = QueryResponse.builder()
            .items(List.of())
            .build();

        when(dynamo.query(any(QueryRequest.class))).thenReturn(emptyResponse);

        Map<String, Object> result = handler.handleRequest(Map.of(), context);

        assertEquals(0, result.get("found"));
        assertEquals(0, result.get("published"));
        assertEquals(0, result.get("failed"));

        verify(sqs, never()).sendMessage(any());
        verify(dynamo, never()).updateItem(any());
    }

    @Test
    void handleRequest_shouldMarkEventAsFailedWhenSqsThrows() {
        Map<String, AttributeValue> item = pendingItem("evt-1", "OrderReceived", "order-1", "{\"orderId\":\"order-1\"}");

        QueryResponse queryResponse = QueryResponse.builder()
            .items(List.of(item))
            .build();

        when(dynamo.query(any(QueryRequest.class))).thenReturn(queryResponse);
        when(sqs.sendMessage(any(SendMessageRequest.class)))
            .thenThrow(new RuntimeException("SQS unavailable"));
        // safeMarkFailed also calls updateItem — allow it
        when(dynamo.updateItem(any(UpdateItemRequest.class))).thenReturn(UpdateItemResponse.builder().build());

        Map<String, Object> result = handler.handleRequest(Map.of(), context);

        assertEquals(1, result.get("found"));
        assertEquals(0, result.get("published"));
        assertEquals(1, result.get("failed"));

        verify(dynamo).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void handleRequest_shouldSkipItemWhenConditionalUpdateFails() {
        // Simulates a race condition: two Lambda instances process the same event;
        // the second one gets ConditionalCheckFailedException on markAsSent.
        Map<String, AttributeValue> item = pendingItem("evt-1", "OrderReceived", "order-1", "{\"orderId\":\"order-1\"}");

        QueryResponse queryResponse = QueryResponse.builder()
            .items(List.of(item))
            .build();

        when(dynamo.query(any(QueryRequest.class))).thenReturn(queryResponse);
        when(dynamo.updateItem(any(UpdateItemRequest.class)))
            .thenThrow(ConditionalCheckFailedException.builder().message("conditional check failed").build());

        Map<String, Object> result = handler.handleRequest(Map.of(), context);

        assertEquals(1, result.get("found"));
        assertEquals(0, result.get("published"));
        assertEquals(1, result.get("skipped")); // not published by this instance
        assertEquals(0, result.get("failed"));

        verify(sqs).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void handleRequest_shouldSkipItemWhenEventIdIsNull() {
        // Item with no eventId is invalid and must be skipped
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("eventType",   AttributeValue.builder().s("OrderReceived").build());
        item.put("aggregateId", AttributeValue.builder().s("order-1").build());
        item.put("payload",     AttributeValue.builder().s("{\"orderId\":\"order-1\"}").build());
        // No "eventId" key

        QueryResponse queryResponse = QueryResponse.builder()
            .items(List.of(item))
            .build();

        when(dynamo.query(any(QueryRequest.class))).thenReturn(queryResponse);

        Map<String, Object> result = handler.handleRequest(Map.of(), context);

        assertEquals(1, result.get("found"));
        assertEquals(0, result.get("published"));
        assertEquals(1, result.get("skipped"));
        assertEquals(0, result.get("failed"));

        verify(sqs, never()).sendMessage(any());
        verify(dynamo, never()).updateItem(any());
    }

    // --- helpers ---

    private Map<String, AttributeValue> pendingItem(String eventId, String eventType,
                                                      String aggregateId, String payload) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("eventId",       AttributeValue.builder().s(eventId).build());
        item.put("eventType",     AttributeValue.builder().s(eventType).build());
        item.put("aggregateId",   AttributeValue.builder().s(aggregateId).build());
        item.put("payload",       AttributeValue.builder().s(payload).build());
        item.put("publishStatus", AttributeValue.builder().s("PENDING").build());
        return item;
    }
}
