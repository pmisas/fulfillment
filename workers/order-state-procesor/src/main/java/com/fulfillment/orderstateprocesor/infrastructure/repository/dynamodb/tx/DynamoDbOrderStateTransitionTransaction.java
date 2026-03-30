package com.fulfillment.orderstateprocesor.infrastructure.repository.dynamodb.tx;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fulfillment.orderstateprocesor.domain.model.Order;
import com.fulfillment.orderstateprocesor.domain.model.OrderItem;
import com.fulfillment.orderstateprocesor.domain.model.OrderStateHistory;
import com.fulfillment.orderstateprocesor.domain.model.Status;
import com.fulfillment.orderstateprocesor.domain.ports.OrderStateTransitionTransaction;


import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;

@Component
@Profile("cloud")
public class DynamoDbOrderStateTransitionTransaction implements OrderStateTransitionTransaction {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbOrderStateTransitionTransaction.class);

    private final DynamoDbAsyncClient dynamoDbAsyncClient;
    private final String ordersTable;
    private final String historyTable;

    public DynamoDbOrderStateTransitionTransaction(
        DynamoDbAsyncClient dynamoDbAsyncClient,
        @Value("${aws.dynamodb.ordersTable}") String ordersTable,
        @Value("${aws.dynamodb.historyTable}") String historyTable
    ) {
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.ordersTable = ordersTable;
        this.historyTable = historyTable;
    }

    @Override
    public Mono<Boolean> transitionIfCurrentStatus(
            Order nextOrder,
            Status expectedCurrentStatus,
            OrderStateHistory history) {

        Map<String, AttributeValue> orderItem = toOrderItemMap(nextOrder);
        Map<String, AttributeValue> historyItem = toHistoryItemMap(history);

        TransactWriteItem putOrder = TransactWriteItem.builder()
            .put(Put.builder()
                .tableName(ordersTable)
                .item(orderItem)
                .conditionExpression("#status = :expectedStatus")
                .expressionAttributeNames(Map.of("#status", "status"))
                .expressionAttributeValues(Map.of(
                    ":expectedStatus", AttributeValue.builder().s(expectedCurrentStatus.name()).build()
                ))
                .build())
            .build();

        TransactWriteItem putHistory = TransactWriteItem.builder()
            .put(Put.builder()
                .tableName(historyTable)
                .item(historyItem)
                .conditionExpression("attribute_not_exists(historyId)")
                .build())
            .build();

        TransactWriteItemsRequest request = TransactWriteItemsRequest.builder()
            .transactItems(List.of(putOrder, putHistory))
            .build();

        return Mono.fromFuture(() -> dynamoDbAsyncClient.transactWriteItems(request))
            .thenReturn(true)
            .onErrorResume(TransactionCanceledException.class, ex -> {
                log.info(
                    "Atomic order transition not applied for orderId={} expectedStatus={} reason={}",
                    nextOrder.getOrderId(),
                    expectedCurrentStatus,
                    ex.getMessage()
                );
                return Mono.just(false);
            });
    }

    private Map<String, AttributeValue> toOrderItemMap(Order order) {
        List<AttributeValue> items = order.getItems().stream()
            .map(this::toOrderItemAttributeValue)
            .toList();

        return Map.of(
            "orderId", AttributeValue.builder().s(order.getOrderId()).build(),
            "operatorId", AttributeValue.builder().s(order.getOperatorId()).build(),
            "warehouseId", AttributeValue.builder().s(order.getWarehouseId() == null ? "" : order.getWarehouseId()).build(),
            "status", AttributeValue.builder().s(order.getStatus().name()).build(),
            "createdAt", AttributeValue.builder().s(order.getCreatedAt().toString()).build(),
            "updatedAt", AttributeValue.builder().s(order.getUpdatedAt().toString()).build(),
            "lat", AttributeValue.builder().n(Double.toString(order.getLat())).build(),
            "lng", AttributeValue.builder().n(Double.toString(order.getLng())).build(),
            "items", AttributeValue.builder().l(items).build()
        );
    }

    private AttributeValue toOrderItemAttributeValue(OrderItem item) {
        return AttributeValue.builder().m(Map.of(
            "sku", AttributeValue.builder().s(item.getSku()).build(),
            "quantity", AttributeValue.builder().n(Integer.toString(item.getQuantity())).build()
        )).build();
    }

    private Map<String, AttributeValue> toHistoryItemMap(OrderStateHistory history) {
        return Map.of(
            "historyId", AttributeValue.builder().s(history.getHistoryId()).build(),
            "orderId", AttributeValue.builder().s(history.getOrderId()).build(),
            "fromStatus", AttributeValue.builder().s(history.getFromStatus() == null ? "" : history.getFromStatus().name()).build(),
            "toStatus", AttributeValue.builder().s(history.getToStatus().name()).build(),
            "changedAt", AttributeValue.builder().s(history.getChangedAt().toString()).build()
        );
    }
}
