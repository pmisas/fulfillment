package com.fulfillment.orderstateprocesor.infrastructure.repository.dynamodb.history;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.orderstateprocesor.domain.model.OrderStateHistory;
import com.fulfillment.orderstateprocesor.domain.ports.OrderStateHistoryRepository;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
@Profile("cloud")
public class DynamoDbOrderStateHistoryRepositoryAdapter implements OrderStateHistoryRepository {

    private final DynamoDbAsyncTable<OrderStateHistoryEntity> table;

    public DynamoDbOrderStateHistoryRepositoryAdapter(
        DynamoDbEnhancedAsyncClient enhancedAsyncClient,
        @Value("${aws.dynamodb.historyTable}") String tableName
    ) {
        this.table = enhancedAsyncClient.table(tableName, TableSchema.fromBean(OrderStateHistoryEntity.class));
    }

    @Override
    public Mono<Void> append(OrderStateHistory history) {
        return Mono.fromFuture(() -> table.putItem(toEntity(history))).then();
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
