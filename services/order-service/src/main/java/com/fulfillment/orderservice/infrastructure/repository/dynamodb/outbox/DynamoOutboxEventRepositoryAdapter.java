package com.fulfillment.orderservice.infrastructure.repository.dynamodb.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.orderservice.domain.ports.OrderWriteTransaction.OutboxPendingEvent;
import com.fulfillment.orderservice.infrastructure.repository.dynamodb.OrderEntityMapper;
import com.fulfillment.orderservice.domain.ports.OutboxEventsRepository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
@Profile("cloud")
public class DynamoOutboxEventRepositoryAdapter implements OutboxEventsRepository {

    private final DynamoDbTable<OutboxEventEntity> table;

    public DynamoOutboxEventRepositoryAdapter(
        DynamoDbEnhancedClient enhancedClient,
        @Value("${aws.dynamodb.outbox-table}") String tableName
    ) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(OutboxEventEntity.class));
    }

    @Override
    public void savePending(OutboxPendingEvent event) {
        table.putItem(OrderEntityMapper.toEntity(event));
    }
}
