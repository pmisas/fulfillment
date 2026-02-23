package com.fulfillment.orderservice.infrastructure.repository.dynamodb.outbox;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.orderservice.domain.ports.OutboxEventsRepository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
@Profile("cloud")
public class DynamoOutboxEventRepositoryAdapter implements OutboxEventsRepository {

    private final DynamoDbTable<OutboxEventEntity> table;

    private static final Duration OUTBOX_TTL = Duration.ofDays(7);

    public DynamoOutboxEventRepositoryAdapter(
        DynamoDbEnhancedClient enhancedClient,
        @Value("${aws.dynamodb.outbox-table}") String tableName
    ) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(OutboxEventEntity.class));
    }

    @Override
    public void savePending(String eventId, String aggregateType, String aggregateId, String eventType, String payload) {
        long nowMs = Instant.now().toEpochMilli();
        long ttlSeconds = Instant.now().plus(OUTBOX_TTL).getEpochSecond();

        OutboxEventEntity e = new OutboxEventEntity();
        e.setEventId(eventId);
        e.setPublishStatus("PENDING");
        e.setCreatedAt(nowMs);
        e.setAttempts(0);

        e.setAggregateType(aggregateType);
        e.setAggregateId(aggregateId);
        e.setEventType(eventType);
        e.setPayload(payload);

        e.setTtl(ttlSeconds);

        table.putItem(e);
    }
}