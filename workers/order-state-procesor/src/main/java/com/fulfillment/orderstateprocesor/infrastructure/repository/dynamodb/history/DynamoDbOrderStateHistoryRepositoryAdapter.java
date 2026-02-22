package com.fulfillment.orderstateprocesor.infrastructure.repository.dynamodb.history;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.fulfillment.orderstateprocesor.domain.model.OrderStateHistory;
import com.fulfillment.orderstateprocesor.domain.ports.OrderStateHistoryRepository;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

@Repository
public class DynamoDbOrderStateHistoryRepositoryAdapter implements OrderStateHistoryRepository {

    private final DynamoDbClient dynamo;
    private final String tableName;

    public DynamoDbOrderStateHistoryRepositoryAdapter(DynamoDbClient dynamo,
        @Value("${aws.dynamodb.historyTable}") String tableName
    ) {
        this.dynamo = dynamo;
        this.tableName = tableName;
    }

    @Override
    public void append(OrderStateHistory history) {
        var item = java.util.Map.<String, AttributeValue>of(
            "historyId", AttributeValue.builder().s(history.getHistoryId()).build(),
            "orderId", AttributeValue.builder().s(history.getOrderId()).build(),
            "fromStatus", AttributeValue.builder().s(history.getFromStatus() == null ? "" : history.getFromStatus().name()).build(),
            "toStatus", AttributeValue.builder().s(history.getToStatus().name()).build(),
            "changedAt", AttributeValue.builder().n(Long.toString(history.getChangedAt().toEpochMilli())).build()
        );

        dynamo.putItem(PutItemRequest.builder()
            .tableName(tableName)
            .item(item)
            .build());
    }
}