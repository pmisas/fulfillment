package com.fulfillment.orderstateprocesor.infrastructure.repository.dynamodb.processed;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.fulfillment.orderstateprocesor.domain.ports.ProcessedEventStore;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

@Repository
public class DynamoDbProcessedEventStoreAdapter implements ProcessedEventStore {

    private final DynamoDbClient dynamo;
    private final String tableName;

    public DynamoDbProcessedEventStoreAdapter(DynamoDbClient dynamo,
        @Value("${aws.dynamodb.processedEventsTable}") String tableName
    ) {
        this.dynamo = dynamo;
        this.tableName = tableName;
    }

    @Override
    public boolean putIfAbsent(String eventId, Duration ttl) {
        long ttlSeconds = Instant.now().plus(ttl).getEpochSecond();

        var item = java.util.Map.<String, AttributeValue>of(
            "eventId", AttributeValue.builder().s(eventId).build(),
            "ttl", AttributeValue.builder().n(Long.toString(ttlSeconds)).build()
        );

        PutItemRequest req = PutItemRequest.builder()
            .tableName(tableName)
            .item(item)
            .conditionExpression("attribute_not_exists(eventId)")
            .build();

        try {
            dynamo.putItem(req);
            return true;
        } catch (ConditionalCheckFailedException e) {
            return false;
        }
    }
}