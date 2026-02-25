package com.fulfillment.orderservice.infrastructure.repository.dynamodb.order;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.domain.ports.OrderRepository;
import com.fulfillment.orderservice.infrastructure.repository.dynamodb.OrderEntityMapper;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
@Profile("cloud")
public class DynamoOrderRepositoryAdapter implements OrderRepository{
    
    private final DynamoDbTable<OrderEntity> table;

    public DynamoOrderRepositoryAdapter(
                DynamoDbEnhancedClient enhancedClient,
                @Value("${aws.dynamodb.orders-table}") String tableName) {
        this.table = enhancedClient.table(tableName, 
            TableSchema.fromBean(OrderEntity.class));
    }

    @Override
    public Order save(Order order) {
        table.putItem(OrderEntityMapper.toEntity(order));
        return order;
    }

    @Override
    public Optional<Order> findById(String orderId) {
        OrderEntity entity = table.getItem(r -> r.key(k -> k.partitionValue(orderId)));
        return Optional.ofNullable(entity).map(OrderEntityMapper::toDomain);
    }

}
