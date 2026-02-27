package com.fulfillment.orderstateprocesor.infrastructure.repository.dynamodb.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.orderstateprocesor.domain.model.Order;
import com.fulfillment.orderstateprocesor.domain.model.OrderItem;
import com.fulfillment.orderstateprocesor.domain.model.Status;
import com.fulfillment.orderstateprocesor.domain.ports.OrderRepository;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
@Profile("cloud")
public class DynamoDbOrderRepositoryAdapter implements OrderRepository {

    private final DynamoDbAsyncTable<OrderEntity> table;

    public DynamoDbOrderRepositoryAdapter(
        DynamoDbEnhancedAsyncClient enhancedAsyncClient,
        @Value("${aws.dynamodb.ordersTable}") String tableName
    ) {
        this.table = enhancedAsyncClient.table(tableName, TableSchema.fromBean(OrderEntity.class));
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
}
