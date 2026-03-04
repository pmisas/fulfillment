package com.fulfillment.orderstateprocesor.infrastructure.repository.dynamodb.order;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.orderstateprocesor.domain.model.Order;
import com.fulfillment.orderstateprocesor.domain.model.OrderItem;
import com.fulfillment.orderstateprocesor.domain.model.Status;
import com.fulfillment.orderstateprocesor.domain.ports.OrderRepository;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
@Profile("cloud")
public class DynamoDbOrderRepositoryAdapter implements OrderRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbOrderRepositoryAdapter.class);

    private final DynamoDbAsyncTable<OrderEntity> table;
    private final DynamoDbAsyncClient lowLevelClient;
    private final String tableName;

    public DynamoDbOrderRepositoryAdapter(
        DynamoDbEnhancedAsyncClient enhancedAsyncClient,
        DynamoDbAsyncClient lowLevelClient,
        @Value("${aws.dynamodb.ordersTable}") String tableName
    ) {
        this.table = enhancedAsyncClient.table(tableName, TableSchema.fromBean(OrderEntity.class));
        this.lowLevelClient = lowLevelClient;
        this.tableName = tableName;
    }

    @Override
    public Mono<Order> save(Order order) {
        return Mono.fromFuture(() -> table.putItem(toEntity(order)))
            .thenReturn(order);
    }

    @Override
    public Mono<Order> findById(String orderId) {
        Key key = Key.builder().partitionValue(orderId).build();
        return Mono.fromFuture(() -> table.getItem(key))
            .map(this::toDomain);
    }

    private OrderEntity toEntity(Order order) {
        OrderEntity e = new OrderEntity();
        e.setOrderId(order.getOrderId());
        e.setWarehouseId(order.getWarehouseId() == null ? "" : order.getWarehouseId());

        e.setStatus(order.getStatus().name());
        e.setLat(order.getLat());
        e.setLng(order.getLng());
        e.setCreatedAt(order.getCreatedAt());
        e.setUpdatedAt(order.getUpdatedAt());

        var items = order.getItems().stream().map(i -> {
            OrderEntity.Item di = new OrderEntity.Item();
            di.setSku(i.getSku());
            di.setQuantity(i.getQuantity());
            return di;
        }).toList();

        e.setItems(items);
        return e;
    }

    private Order toDomain(OrderEntity e) {
        java.util.List<OrderItem> items = (e.getItems() == null ? java.util.List.<OrderItem>of()
            : e.getItems().stream()
                .map(i -> OrderItem.createOrderItem(i.getSku(), i.getQuantity()))
                .toList());

        String warehouse = (e.getWarehouseId() == null || e.getWarehouseId().isBlank())
                ? null
                : e.getWarehouseId();

        Double lat = e.getLat();
        Double lng = e.getLng();

        if (lat == null || lng == null) {
            throw new IllegalStateException("Order " + e.getOrderId() + " has null lat/lng in DB");
        }

        return Order.restore(
            e.getOrderId(),
            warehouse,
            Status.valueOf(e.getStatus()),
            lat,
            lng,
            e.getCreatedAt(),
            e.getUpdatedAt(),
            items
        );
    }

    @Override
    public Mono<Boolean> saveIfStatusIs(Order order, Status expectedStatus) {
        OrderEntity entity = toEntity(order);
        Map<String, AttributeValue> item = table.tableSchema().itemToMap(entity, false);

        PutItemRequest request = PutItemRequest.builder()
            .tableName(tableName)
            .item(item)
            .conditionExpression("#status = :expectedStatus")
            .expressionAttributeNames(Map.of("#status", "status"))
            .expressionAttributeValues(Map.of(
                ":expectedStatus", AttributeValue.builder().s(expectedStatus.name()).build()
            ))
            .build();

        return Mono.fromFuture(lowLevelClient.putItem(request))
            .thenReturn(true)
            .onErrorResume(ConditionalCheckFailedException.class, ex -> {
                log.info("Conditional update failed for order={}: status is no longer {}", 
                         order.getOrderId(), expectedStatus);
                return Mono.just(false);
            });
    }
}
