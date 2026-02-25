package com.fulfillment.orderservice.infrastructure.repository.dynamodb.tx;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.domain.model.OrderStateHistory;
import com.fulfillment.orderservice.domain.ports.OrderWriteTransaction;
import com.fulfillment.orderservice.infrastructure.repository.dynamodb.OrderEntityMapper;
import com.fulfillment.orderservice.infrastructure.repository.dynamodb.history.OrderStateHistoryEntity;
import com.fulfillment.orderservice.infrastructure.repository.dynamodb.order.OrderEntity;
import com.fulfillment.orderservice.infrastructure.repository.dynamodb.outbox.OutboxEventEntity;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;

@Component
@Profile("cloud")
public class DynamoOrderWriteTransaction implements OrderWriteTransaction {

    private static final Expression ORDER_MUST_NOT_EXIST =
            Expression.builder().expression("attribute_not_exists(orderId)").build();

    private static final Expression HISTORY_MUST_NOT_EXIST =
            Expression.builder().expression("attribute_not_exists(orderId)").build();

    private static final Expression OUTBOX_MUST_NOT_EXIST =
            Expression.builder().expression("attribute_not_exists(eventId)").build();

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<OrderEntity> ordersTable;
    private final DynamoDbTable<OrderStateHistoryEntity> historyTable;
    private final DynamoDbTable<OutboxEventEntity> outboxTable;

    public DynamoOrderWriteTransaction(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${aws.dynamodb.orders-table}")  String ordersTableName,
            @Value("${aws.dynamodb.history-table}") String historyTableName,
            @Value("${aws.dynamodb.outbox-table}")  String outboxTableName) {

        this.enhancedClient = enhancedClient;
        this.ordersTable  = enhancedClient.table(ordersTableName,  TableSchema.fromBean(OrderEntity.class));
        this.historyTable = enhancedClient.table(historyTableName, TableSchema.fromBean(OrderStateHistoryEntity.class));
        this.outboxTable  = enhancedClient.table(outboxTableName,  TableSchema.fromBean(OutboxEventEntity.class));
    }

    @Override
    public void createOrderWithHistoryAndOutbox(
            Order order,
            OrderStateHistory initialHistory,
            OutboxPendingEvent outboxEvent) {

        PutItemEnhancedRequest<OrderEntity> putOrder =
                PutItemEnhancedRequest.builder(OrderEntity.class)
                        .item(OrderEntityMapper.toEntity(order))
                        .conditionExpression(ORDER_MUST_NOT_EXIST)
                        .build();

        PutItemEnhancedRequest<OrderStateHistoryEntity> putHistory =
                PutItemEnhancedRequest.builder(OrderStateHistoryEntity.class)
                        .item(OrderEntityMapper.toEntity(initialHistory))
                        .conditionExpression(HISTORY_MUST_NOT_EXIST)
                        .build();

        PutItemEnhancedRequest<OutboxEventEntity> putOutbox =
                PutItemEnhancedRequest.builder(OutboxEventEntity.class)
                        .item(OrderEntityMapper.toEntity(outboxEvent))
                        .conditionExpression(OUTBOX_MUST_NOT_EXIST)
                        .build();

        TransactWriteItemsEnhancedRequest tx = TransactWriteItemsEnhancedRequest.builder()
                .addPutItem(ordersTable, putOrder)
                .addPutItem(historyTable, putHistory)
                .addPutItem(outboxTable, putOutbox)
                .build();

        enhancedClient.transactWriteItems(tx);
    }
}
