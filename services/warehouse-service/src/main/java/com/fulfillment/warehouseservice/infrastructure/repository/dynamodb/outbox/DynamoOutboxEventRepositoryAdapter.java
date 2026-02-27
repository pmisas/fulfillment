package com.fulfillment.warehouseservice.infrastructure.repository.dynamodb.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.warehouseservice.domain.port.OutboxEventsRepository;
import com.fulfillment.warehouseservice.infrastructure.repository.dynamodb.WarehouseEntityMapper;

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
        table.putItem(WarehouseEntityMapper.toEntity(event));
    }
}
