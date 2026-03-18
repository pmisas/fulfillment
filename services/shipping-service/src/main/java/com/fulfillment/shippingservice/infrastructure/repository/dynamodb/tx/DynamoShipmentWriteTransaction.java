package com.fulfillment.shippingservice.infrastructure.repository.dynamodb.tx;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fulfillment.shippingservice.domain.model.Shipment;
import com.fulfillment.shippingservice.domain.model.ShipmentStatus;
import com.fulfillment.shippingservice.domain.ports.OutboxEventsRepository.OutboxPendingEvent;
import com.fulfillment.shippingservice.domain.ports.ShipmentWriteTransaction;
import com.fulfillment.shippingservice.infrastructure.repository.dynamodb.ShipmentEntityMapper;
import com.fulfillment.shippingservice.infrastructure.repository.dynamodb.outbox.OutboxEventEntity;
import com.fulfillment.shippingservice.infrastructure.repository.dynamodb.shipment.ShipmentEntity;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

@Component
@Profile("cloud")
public class DynamoShipmentWriteTransaction implements ShipmentWriteTransaction {

    private static final Logger log = LoggerFactory.getLogger(DynamoShipmentWriteTransaction.class);

    private static final Duration OUTBOX_TTL = Duration.ofDays(7);

    private static final Expression OUTBOX_MUST_NOT_EXIST =
        Expression.builder().expression("attribute_not_exists(eventId)").build();

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<ShipmentEntity> shipmentsTable;
    private final DynamoDbTable<OutboxEventEntity> outboxTable;

    public DynamoShipmentWriteTransaction(
        DynamoDbEnhancedClient enhancedClient,
        @Value("${aws.dynamodb.shipments-table}") String shipmentsTableName,
        @Value("${aws.dynamodb.outbox-table}") String outboxTableName
    ) {
        this.enhancedClient = enhancedClient;
        this.shipmentsTable = enhancedClient.table(shipmentsTableName, TableSchema.fromBean(ShipmentEntity.class));
        this.outboxTable    = enhancedClient.table(outboxTableName,    TableSchema.fromBean(OutboxEventEntity.class));
    }

    @Override
    public Optional<Shipment> saveStatusWithOutbox(
            Shipment shipment,
            ShipmentStatus expectedCurrentStatus,
            OutboxPendingEvent event) {

        Expression shipmentCondition = Expression.builder()
            .expression("#s = :expected")
            .putExpressionName("#s", "status")
            .putExpressionValue(":expected", AttributeValue.fromS(expectedCurrentStatus.name()))
            .build();

        TransactPutItemEnhancedRequest<ShipmentEntity> putShipment =
            TransactPutItemEnhancedRequest.builder(ShipmentEntity.class)
                .item(ShipmentEntityMapper.toEntity(shipment))
                .conditionExpression(shipmentCondition)
                .build();

        TransactPutItemEnhancedRequest<OutboxEventEntity> putOutbox =
            TransactPutItemEnhancedRequest.builder(OutboxEventEntity.class)
                .item(toOutboxEntity(event))
                .conditionExpression(OUTBOX_MUST_NOT_EXIST)
                .build();

        TransactWriteItemsEnhancedRequest tx = TransactWriteItemsEnhancedRequest.builder()
            .addPutItem(shipmentsTable, putShipment)
            .addPutItem(outboxTable, putOutbox)
            .build();

        try {
            enhancedClient.transactWriteItems(tx);
            log.info("Shipment status+outbox written atomically: shipmentId={} status={}",
                shipment.getShipmentId(), shipment.getStatus());
            return Optional.of(shipment);
        } catch (TransactionCanceledException e) {
            log.info("Transaction cancelled for shipmentId={} (concurrent status change or duplicate event): {}",
                shipment.getShipmentId(), e.getMessage());
            return Optional.empty();
        }
    }

    private OutboxEventEntity toOutboxEntity(OutboxPendingEvent evt) {
        long nowMs      = Instant.now().toEpochMilli();
        long ttlSeconds = Instant.now().plus(OUTBOX_TTL).getEpochSecond();

        OutboxEventEntity e = new OutboxEventEntity();
        e.setEventId(evt.eventId());
        e.setAggregateType(evt.aggregateType());
        e.setAggregateId(evt.aggregateId());
        e.setEventType(evt.eventType());
        e.setPayload(evt.payload());
        e.setPublishStatus("PENDING");
        e.setCreatedAt(nowMs);
        e.setAttempts(0);
        e.setTtl(ttlSeconds);
        return e;
    }
}
