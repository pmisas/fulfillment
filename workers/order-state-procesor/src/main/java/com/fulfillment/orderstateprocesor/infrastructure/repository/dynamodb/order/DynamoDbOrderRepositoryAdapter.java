package com.fulfillment.orderstateprocesor.infrastructure.repository.dynamodb.order;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.orderstateprocesor.domain.model.Order;
import com.fulfillment.orderstateprocesor.domain.model.OrderItem;
import com.fulfillment.orderstateprocesor.domain.model.Status;
import com.fulfillment.orderstateprocesor.domain.ports.OrderRepository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
@Profile("cloud")
public class DynamoDbOrderRepositoryAdapter implements OrderRepository {

    private final DynamoDbTable<OrderEntity> table;

    public DynamoDbOrderRepositoryAdapter(
        DynamoDbEnhancedClient enhancedClient,
        @Value("${aws.dynamodb.ordersTable}") String tableName
    ) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(OrderEntity.class));
    }

    @Override
    public Order save(Order order) {
        table.putItem(toEntity(order));
        return order;
    }

    @Override
    public Optional<Order> findById(String orderId) {
        OrderEntity entity = table.getItem(r -> r.key(k -> k.partitionValue(orderId)));
        return Optional.ofNullable(entity).map(this::toDomain);
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
        var items = (e.getItems() == null ? java.util.List.<OrderItem>of()
            : e.getItems().stream()
                .map(i -> OrderItem.create(i.getSku(), i.getQuantity()))
                .toList());

        return Order.restore(
            e.getOrderId(),
            e.getWarehouseId(),
            Status.valueOf(e.getStatus()),
            e.getLat() != null ? e.getLat() : 0.0,
            e.getLng() != null ? e.getLng() : 0.0,
            e.getCreatedAt(),
            e.getUpdatedAt(),
            items
        );
    }
}
