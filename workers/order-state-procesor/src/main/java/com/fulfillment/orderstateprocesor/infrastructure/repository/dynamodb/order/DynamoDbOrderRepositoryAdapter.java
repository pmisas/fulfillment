package com.fulfillment.orderstateprocesor.infrastructure.repository.dynamodb.order;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.fulfillment.orderstateprocesor.domain.model.Order;
import com.fulfillment.orderstateprocesor.domain.model.OrderItem;
import com.fulfillment.orderstateprocesor.domain.model.Status;
import com.fulfillment.orderstateprocesor.domain.ports.OrderRepository;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

@Repository
public class DynamoDbOrderRepositoryAdapter implements OrderRepository {

    private final DynamoDbClient dynamo;
    private final String tableName;

    public DynamoDbOrderRepositoryAdapter(DynamoDbClient dynamo, @Value("${aws.dynamodb.ordersTable}") String tableName) {
        this.dynamo = dynamo;
        this.tableName = tableName;
    }

    @Override
    public Optional<Order> findById(String orderId) {
        GetItemRequest req = GetItemRequest.builder()
            .tableName(tableName)
            .key(java.util.Map.of("orderId", AttributeValue.builder().s(orderId).build()))
            .build();

        GetItemResponse resp = dynamo.getItem(req);
        if (!resp.hasItem()) return Optional.empty();

        return Optional.of(fromItem(resp.item()));
    }

    @Override
    public Order save(Order order) {
        PutItemRequest req = PutItemRequest.builder()
            .tableName(tableName)
            .item(toItem(order))
            .build();

        dynamo.putItem(req);
        return order;
    }

    private Order fromItem(java.util.Map<String, AttributeValue> item) {
        String orderId = item.get("orderId").s();
        String customerId = item.get("customerId").s();
        String warehouseId = item.containsKey("warehouseId") ? item.get("warehouseId").s() : "";
        Status status = Status.valueOf(item.get("status").s());

        Instant createdAt = Instant.ofEpochMilli(Long.parseLong(item.get("createdAt").n()));
        Instant updatedAt = Instant.ofEpochMilli(Long.parseLong(item.get("updatedAt").n()));

        List<OrderItem> items = new ArrayList<>();
        if (item.containsKey("items") && item.get("items").l() != null) {
            for (AttributeValue av : item.get("items").l()) {
                var m = av.m();
                String sku = m.get("sku").s();
                int qty = Integer.parseInt(m.get("quantity").n());
                items.add(OrderItem.create(sku, qty));
            }
        }

        return Order.restore(orderId, customerId, warehouseId, status, createdAt, updatedAt, items);
    }

    private java.util.Map<String, AttributeValue> toItem(Order order) {
        List<AttributeValue> items = order.getItems().stream()
            .map(i -> AttributeValue.builder().m(java.util.Map.of(
                "sku", AttributeValue.builder().s(i.getSku()).build(),
                "quantity", AttributeValue.builder().n(Integer.toString(i.getQuantity())).build()
            )).build())
            .toList();

        return java.util.Map.of(
            "orderId", AttributeValue.builder().s(order.getOrderId()).build(),
            "customerId", AttributeValue.builder().s(order.getCustomerId()).build(),
            "warehouseId", AttributeValue.builder().s(order.getWarehouseId() == null ? "" : order.getWarehouseId()).build(),
            "status", AttributeValue.builder().s(order.getStatus().name()).build(),
            "createdAt", AttributeValue.builder().n(Long.toString(order.getCreatedAt().toEpochMilli())).build(),
            "updatedAt", AttributeValue.builder().n(Long.toString(order.getUpdatedAt().toEpochMilli())).build(),
            "items", AttributeValue.builder().l(items).build()
        );
    }
}