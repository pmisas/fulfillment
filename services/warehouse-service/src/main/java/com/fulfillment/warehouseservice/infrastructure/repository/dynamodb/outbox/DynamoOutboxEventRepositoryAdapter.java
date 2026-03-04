package com.fulfillment.warehouseservice.infrastructure.repository.dynamodb.outbox;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.warehouseservice.domain.port.OutboxEventsRepository;
import com.fulfillment.warehouseservice.infrastructure.repository.dynamodb.WarehouseEntityMapper;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@Repository
@Profile("cloud")
public class DynamoOutboxEventRepositoryAdapter implements OutboxEventsRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoOutboxEventRepositoryAdapter.class);

    private static final Expression EVENT_MUST_NOT_EXIST =
        Expression.builder().expression("attribute_not_exists(eventId)").build();

    private final DynamoDbTable<OutboxEventEntity> table;
    private final DynamoDbClient dynamoClient;
    private final String tableName;

    public DynamoOutboxEventRepositoryAdapter(
        DynamoDbEnhancedClient enhancedClient,
        DynamoDbClient dynamoClient,
        @Value("${aws.dynamodb.outbox-table}") String tableName
    ) {
        this.tableName = tableName;
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(OutboxEventEntity.class));
        this.dynamoClient = dynamoClient;
        log.info("DynamoOutboxEventRepositoryAdapter initialized with table: {}", tableName);
    }

    @Override
    public boolean savePendingIfAbsent(OutboxPendingEvent event) {
        log.debug("Attempting to save outbox event: eventId={}, eventType={}, aggregateId={}, tableName={}", 
                  event.eventId(), event.eventType(), event.aggregateId(), tableName);
        
        try {
            var req = PutItemEnhancedRequest.builder(OutboxEventEntity.class)
                .item(WarehouseEntityMapper.toEntity(event))
                .conditionExpression(EVENT_MUST_NOT_EXIST)
                .build();

            table.putItem(req);
            log.info("Successfully saved outbox event: eventId={} to table={}", event.eventId(), tableName);
            return true;
        } catch (ConditionalCheckFailedException e) {
            log.warn("Outbox event already exists (idempotent): eventId={}, table={}", event.eventId(), tableName);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error saving outbox event: eventId={}, table={}, error={}", 
                      event.eventId(), tableName, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean resetToPendingIfProcessed(String eventId) {
        log.info("Attempting to reset event to PENDING: eventId={}, table={}", eventId, tableName);
        
        long nowMs = Instant.now().toEpochMilli();
        
        try {
            UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(java.util.Map.of(
                    "eventId", AttributeValue.builder().s(eventId).build()
                ))
                .updateExpression("SET publishStatus = :pending, createdAt = :now, attempts = :zero REMOVE publishedAt, lastError")
                .conditionExpression("attribute_exists(eventId) AND publishStatus IN (:sent, :failed)")
                .expressionAttributeValues(java.util.Map.of(
                    ":pending", AttributeValue.builder().s("PENDING").build(),
                    ":sent", AttributeValue.builder().s("SENT").build(),
                    ":failed", AttributeValue.builder().s("FAILED").build(),
                    ":now", AttributeValue.builder().n(String.valueOf(nowMs)).build(),
                    ":zero", AttributeValue.builder().n("0").build()
                ))
                .build();
            
            dynamoClient.updateItem(request);
            log.info("Successfully reset event to PENDING: eventId={}, table={}", eventId, tableName);
            return true;
            
        } catch (ConditionalCheckFailedException e) {
            log.debug("Event not reset - either doesn't exist or already PENDING: eventId={}, table={}", 
                     eventId, tableName);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error resetting event: eventId={}, table={}, error={}", 
                      eventId, tableName, e.getMessage(), e);
            throw e;
        }
    }
}