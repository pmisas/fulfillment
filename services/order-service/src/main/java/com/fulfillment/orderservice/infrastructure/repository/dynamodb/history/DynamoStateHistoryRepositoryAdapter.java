package com.fulfillment.orderservice.infrastructure.repository.dynamodb.history;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.orderservice.domain.model.OrderStateHistory;
import com.fulfillment.orderservice.domain.ports.OrderStateHistoryRepository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Repository
@Profile("cloud")
public class DynamoStateHistoryRepositoryAdapter implements OrderStateHistoryRepository{
    
    private final DynamoDbTable<OrderStateHistoryEntity> table;

    public DynamoStateHistoryRepositoryAdapter(
                DynamoDbEnhancedClient enhancedClient,
                @Value("${aws.dynamodb.history-table}") String tableName) {
        this.table = enhancedClient.table(tableName, 
            TableSchema.fromBean(OrderStateHistoryEntity.class));
    }

    @Override
    public void append(OrderStateHistory history) {
        table.putItem(toEntity(history));
    }

    @Override
    public List<OrderStateHistory> findByOrderId(String orderId) {
        return table.query(r -> r.queryConditional(
                QueryConditional.keyEqualTo(k -> k.partitionValue(orderId))
            )).stream()
            .flatMap(page -> page.items().stream())
            .map(this::toDomain)
            .toList();
    }
    

    private OrderStateHistoryEntity toEntity(OrderStateHistory history) {
        OrderStateHistoryEntity e = new OrderStateHistoryEntity();
        e.setOrderId(history.getOrderId());
        e.setChangedAt(history.getchangedAt());
        e.setHistoryId(history.getHistoryId());
        e.setFromStatus(history.getFromStatus());
        e.setToStatus(history.getToStatus());
        return e;
    }

    private OrderStateHistory toDomain(OrderStateHistoryEntity e) {
        return OrderStateHistory.restore(
            e.getHistoryId(),
            e.getOrderId(),
            e.getFromStatus(),
            e.getToStatus(),
            e.getChangedAt()
        );
    }

}
