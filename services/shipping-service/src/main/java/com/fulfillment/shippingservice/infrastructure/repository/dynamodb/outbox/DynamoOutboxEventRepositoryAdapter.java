package com.fulfillment.shippingservice.infrastructure.repository.dynamodb.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.shippingservice.domain.ports.OutboxEventsRepository;

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
    private static final Duration OUTBOX_TTL = Duration.ofDays(7);

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
    }

    @Override
    public boolean savePendingIfAbsent(OutboxPendingEvent event) {
        try {
            var req = PutItemEnhancedRequest.builder(OutboxEventEntity.class)
                .item(toEntity(event))
                .conditionExpression(EVENT_MUST_NOT_EXIST)
                .build();

            table.putItem(req);
            log.info("Outbox event saved: eventId={}", event.eventId());
            return true;
        } catch (ConditionalCheckFailedException e) {
            log.warn("Outbox event already exists (idempotent): eventId={}", event.eventId());
            return false;
        }
    }

    @Override
    public boolean resetToPendingIfProcessed(String eventId) {
        long nowMs = Instant.now().toEpochMilli();
        try {
            UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("eventId", AttributeValue.builder().s(eventId).build()))
                .updateExpression("SET publishStatus = :pending, createdAt = :now, attempts = :zero REMOVE publishedAt, lastError")
                .conditionExpression("attribute_exists(eventId) AND publishStatus IN (:sent, :failed)")
                .expressionAttributeValues(Map.of(
                    ":pending", AttributeValue.builder().s("PENDING").build(),
                    ":sent",    AttributeValue.builder().s("SENT").build(),
                    ":failed",  AttributeValue.builder().s("FAILED").build(),
                    ":now",     AttributeValue.builder().n(String.valueOf(nowMs)).build(),
                    ":zero",    AttributeValue.builder().n("0").build()
                ))
                .build();

            dynamoClient.updateItem(request);
            log.info("Outbox event reset to PENDING: eventId={}", eventId);
            return true;
        } catch (ConditionalCheckFailedException e) {
            log.debug("Event not reset - either not found or already PENDING: eventId={}", eventId);
            return false;
        }
    }

    private OutboxEventEntity toEntity(OutboxPendingEvent evt) {
        long nowMs      = Instant.now().toEpochMilli();
        long ttlSeconds = Instant.now().plus(OUTBOX_TTL).getEpochSecond();

        OutboxEventEntity e = new OutboxEventEntity();
        e.setEventId(evt.eventId());
        e.setAggregateType(evt.aggregateType());
        e.setAggregateId(evt.aggregateId());
        e.setEventType(evt.eventType());
        e.setPayload(evt.payload());
        e.setPublishStatus("PENDING");
        e.setCreatedAt(nowMs);
        e.setAttempts(0);
        e.setTtl(ttlSeconds);
        return e;
    }
}
