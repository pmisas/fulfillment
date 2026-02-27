package com.fulfillment.warehouseservice.infrastructure.repository.dynamodb.outbox;

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
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@Repository
@Profile("cloud")
public class DynamoOutboxEventRepositoryAdapter implements OutboxEventsRepository {

    private static final Expression EVENT_MUST_NOT_EXIST =
        Expression.builder().expression("attribute_not_exists(eventId)").build();

    private final DynamoDbTable<OutboxEventEntity> table;

    public DynamoOutboxEventRepositoryAdapter(
        DynamoDbEnhancedClient enhancedClient,
        @Value("${aws.dynamodb.outbox-table}") String tableName
    ) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(OutboxEventEntity.class));
    }

    @Override
    public boolean savePendingIfAbsent(OutboxPendingEvent event) {
        try {
            var req = PutItemEnhancedRequest.builder(OutboxEventEntity.class)
                .item(WarehouseEntityMapper.toEntity(event))
                .conditionExpression(EVENT_MUST_NOT_EXIST)
                .build();

            table.putItem(req);
            return true;
        } catch (ConditionalCheckFailedException e) {
            return false; // ya existía
        }
    }
}