package com.fulfillment.orderstateprocesor.infrastructure.repository.dynamodb.processed;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.fulfillment.orderstateprocesor.domain.ports.ProcessedEventStore;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@Repository
public class DynamoDbProcessedEventStoreAdapter implements ProcessedEventStore {

    private final DynamoDbTable<ProcessedEventEntity> table;

    public DynamoDbProcessedEventStoreAdapter(
        DynamoDbEnhancedClient enhancedClient,
        @Value("${aws.dynamodb.processedEventsTable}") String tableName
    ) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(ProcessedEventEntity.class));
    }

    @Override
    public boolean putIfAbsent(String eventId, Duration ttl) {
        long ttlSeconds = Instant.now().plus(ttl).getEpochSecond();

        ProcessedEventEntity entity = new ProcessedEventEntity();
        entity.setEventId(eventId);
        entity.setTtl(ttlSeconds);

        try {
            // Condición: solo insert si NO existe
            table.putItem(r -> r
                .item(entity)
                .conditionExpression(
                    software.amazon.awssdk.enhanced.dynamodb.Expression.builder()
                        .expression("attribute_not_exists(eventId)")
                        .build()
                )
            );
            return true;
        } catch (ConditionalCheckFailedException e) {
            return false;
        }
    }
}
