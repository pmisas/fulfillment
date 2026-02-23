package com.fulfillment.orderstateprocesor.infrastructure.repository.dynamodb.history;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.orderstateprocesor.domain.model.OrderStateHistory;
import com.fulfillment.orderstateprocesor.domain.ports.OrderStateHistoryRepository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
@Profile("cloud")
public class DynamoDbOrderStateHistoryRepositoryAdapter implements OrderStateHistoryRepository {

    private final DynamoDbTable<OrderStateHistoryEntity> table;

    public DynamoDbOrderStateHistoryRepositoryAdapter(
        DynamoDbEnhancedClient enhancedClient,
        @Value("${aws.dynamodb.historyTable}") String tableName
    ) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(OrderStateHistoryEntity.class));
    }

    @Override
    public void append(OrderStateHistory history) {
        table.putItem(toEntity(history));
    }

    private OrderStateHistoryEntity toEntity(OrderStateHistory h) {
        OrderStateHistoryEntity e = new OrderStateHistoryEntity();
        e.setHistoryId(h.getHistoryId());
        e.setOrderId(h.getOrderId());
        e.setFromStatus(h.getFromStatus() == null ? "" : h.getFromStatus().name());
        e.setToStatus(h.getToStatus().name());
        e.setChangedAt(h.getChangedAt());
        return e;
    }
}
