package com.fulfillment.orderservice.infrastructure.repository.dynamodb.order;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.domain.model.Status;
import com.fulfillment.orderservice.domain.ports.OrderRepository;
import com.fulfillment.orderservice.infrastructure.repository.dynamodb.OrderEntityMapper;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Repository
@Profile("cloud")
public class DynamoOrderRepositoryAdapter implements OrderRepository {

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

    @Override
    public List<Order> findAll() {
        return table.scan()
            .items()
            .stream()
            .map(OrderEntityMapper::toDomain)
            .toList();
    }

    public List<Order> findByOperatorId(String operatorId) {
        return scanWithFilter(Expression.builder()
            .expression("#op = :op")
            .expressionNames(Map.of("#op", "operatorId"))
            .expressionValues(Map.of(":op", AttributeValue.fromS(operatorId)))
            .build());
    }

    @Override
    public List<Order> findByOperatorIdAndStatus(String operatorId, Status status) {
        return scanWithFilter(Expression.builder()
            .expression("#op = :op AND #st = :st")
            .expressionNames(Map.of("#op", "operatorId", "#st", "status"))
            .expressionValues(Map.of(
                ":op", AttributeValue.fromS(operatorId),
                ":st", AttributeValue.fromS(status.name())))
            .build());
    }

    @Override
    public List<Order> findByOperatorIdAndWarehouseId(String operatorId, String warehouseId) {
        return scanWithFilter(Expression.builder()
            .expression("#op = :op AND #wh = :wh")
            .expressionNames(Map.of("#op", "operatorId", "#wh", "warehouseId"))
            .expressionValues(Map.of(
                ":op", AttributeValue.fromS(operatorId),
                ":wh", AttributeValue.fromS(warehouseId)))
            .build());
    }

    @Override
    public List<Order> findByStatus(Status status) {
        return scanWithFilter(Expression.builder()
            .expression("#st = :st")
            .expressionNames(Map.of("#st", "status"))
            .expressionValues(Map.of(":st", AttributeValue.fromS(status.name())))
            .build());
    }

    @Override
    public List<Order> findByWarehouseId(String warehouseId) {
        return scanWithFilter(Expression.builder()
            .expression("#wh = :wh")
            .expressionNames(Map.of("#wh", "warehouseId"))
            .expressionValues(Map.of(":wh", AttributeValue.fromS(warehouseId)))
            .build());
    }

    private List<Order> scanWithFilter(Expression filter) {
        return table.scan(ScanEnhancedRequest.builder()
                .filterExpression(filter)
                .build())
            .items()
            .stream()
            .map(OrderEntityMapper::toDomain)
            .toList();
    }
}

