package com.fulfillment.inventoryservice.infraestrcture.repository.dynamodb.inventoryReservation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fulfillment.inventoryservice.domain.ports.InventoryReservationTransaction;
import com.fulfillment.inventoryservice.domain.ports.InventoryRestockTransaction;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.Update;

@Component
@Profile("cloud")
public class DynamoInventoryTransactions
    implements InventoryReservationTransaction, InventoryRestockTransaction {

    private static final Logger log = LoggerFactory.getLogger(DynamoInventoryTransactions.class);
    private static final int RESERVATION_CONDITION_INDEX = 0;

    private final DynamoDbClient ddbClient;

    private final DynamoDbTable<InventoryReservationEntity> reservationTable;

    private final String inventoryTableName;
    private final String reservationTableName;

    public DynamoInventoryTransactions(
            DynamoDbClient ddbClient,
            DynamoDbEnhancedClient enhancedClient,
            @Value("${aws.dynamodb.inventory-table}")    String inventoryTableName,
            @Value("${aws.dynamodb.reservation-table}")  String reservationTableName) {

        this.ddbClient = ddbClient;
        this.inventoryTableName = inventoryTableName;
        this.reservationTableName = reservationTableName;

        this.reservationTable = enhancedClient.table(
                reservationTableName,
                TableSchema.fromBean(InventoryReservationEntity.class)
        );
    }


    @Override
    public ReserveResult reserveAtomically(com.fulfillment.inventoryservice.domain.model.InventoryReservation reservation) {
        List<TransactWriteItem> txItems = new ArrayList<>();

        txItems.add(TransactWriteItem.builder()
                .put(Put.builder()
                        .tableName(reservationTableName)
                        .item(toReservationDdbItem(reservation))
                        .conditionExpression("attribute_not_exists(reservationId)")
                        .build())
                .build());

        for (var item : reservation.getItems()) {
            txItems.add(TransactWriteItem.builder()
                    .update(Update.builder()
                            .tableName(inventoryTableName)
                            .key(Map.of(
                                    "warehouseId", AttributeValue.fromS(reservation.getWarehouseId()),
                                    "sku", AttributeValue.fromS(item.sku())
                            ))
                            .updateExpression("SET reserved = if_not_exists(reserved, :zero) + :qty")
                            .conditionExpression("attribute_exists(warehouseId)")
                            .expressionAttributeValues(Map.of(
                                    ":qty", AttributeValue.fromN(String.valueOf(item.quantity())),
                                    ":zero", AttributeValue.fromN("0")
                            ))
                            .build())
                    .build());
        }

        try {
            ddbClient.transactWriteItems(
                    TransactWriteItemsRequest.builder().transactItems(txItems).build());
            return ReserveResult.RESERVED;

        } catch (TransactionCanceledException e) {
            boolean reservationExists = e.cancellationReasons() != null
                    && e.cancellationReasons().size() > RESERVATION_CONDITION_INDEX
                    && "ConditionalCheckFailed".equals(e.cancellationReasons().get(RESERVATION_CONDITION_INDEX).code());

            return reservationExists ? ReserveResult.ALREADY_RESERVED : ReserveResult.INSUFFICIENT_STOCK;
        }
    }


    @Override
    public void releaseAtomically(String reservationId) {
        log.info("Attempting to release reservation: reservationId={}", reservationId);
        
        InventoryReservationEntity entity = reservationTable.getItem(
                Key.builder().partitionValue(reservationId).build());

        if (entity == null) {
            log.warn("Reservation not found (may have been already released): reservationId={}", reservationId);
            return; 
        }

        log.info("Found reservation: reservationId={}, warehouseId={}, orderId={}, items={}", 
                 reservationId, entity.getWarehouseId(), entity.getOrderId(), entity.getItems().size());

        List<TransactWriteItem> txItems = new ArrayList<>();

        txItems.add(TransactWriteItem.builder()
                .delete(Delete.builder()
                        .tableName(reservationTableName)
                        .key(Map.of("reservationId", AttributeValue.fromS(reservationId)))
                        .build())
                .build());

        for (InventoryReservationEntity.Item item : entity.getItems()) {
            log.info("Releasing inventory item: warehouseId={}, sku={}, quantity={}", 
                     entity.getWarehouseId(), item.getSku(), item.getQuantity());
            
            txItems.add(TransactWriteItem.builder()
                    .update(Update.builder()
                            .tableName(inventoryTableName)
                            .key(Map.of(
                                    "warehouseId", AttributeValue.fromS(entity.getWarehouseId()),
                                    "sku", AttributeValue.fromS(item.getSku())
                            ))
                            .updateExpression("SET reserved = if_not_exists(reserved, :zero) - :qty")
                            .conditionExpression("attribute_exists(reserved) AND reserved >= :qty")
                            .expressionAttributeValues(Map.of(
                                    ":qty", AttributeValue.fromN(String.valueOf(item.getQuantity())),
                                    ":zero", AttributeValue.fromN("0")
                            ))
                            .build())
                    .build());
        }

        log.info("Executing DynamoDB transaction with {} write items", txItems.size());
        
        try {
            ddbClient.transactWriteItems(
                    TransactWriteItemsRequest.builder().transactItems(txItems).build());
            log.info("Successfully released reservation: reservationId={}", reservationId);
        } catch (TransactionCanceledException e) {
            log.error("DynamoDB Transaction CANCELED while releasing reservation: reservationId={}", reservationId);
            log.error("Cancellation reasons: {}", e.cancellationReasons());
            log.error("Full exception: ", e);
            throw new IllegalStateException("Failed to release reservation due to transaction cancellation: " + reservationId, e);
        } catch (Exception e) {
            log.error("UNEXPECTED error while releasing reservation: reservationId={}, errorType={}, message={}", 
                      reservationId, e.getClass().getSimpleName(), e.getMessage());
            log.error("Full exception: ", e);
            throw new IllegalStateException("Failed to release reservation: " + reservationId, e);
        }
    }


    @Override
    public ConsumeResult consumeAtomically(String reservationId) {
        InventoryReservationEntity entity = reservationTable.getItem(
                Key.builder().partitionValue(reservationId).build());

        if (entity == null) return ConsumeResult.RESERVATION_NOT_FOUND;

        List<TransactWriteItem> txItems = new ArrayList<>();

        txItems.add(TransactWriteItem.builder()
                .delete(Delete.builder()
                        .tableName(reservationTableName)
                        .key(Map.of("reservationId", AttributeValue.fromS(reservationId)))
                        .build())
                .build());

        for (InventoryReservationEntity.Item item : entity.getItems()) {
            txItems.add(TransactWriteItem.builder()
                    .update(Update.builder()
                            .tableName(inventoryTableName)
                            .key(Map.of(
                                    "warehouseId", AttributeValue.fromS(entity.getWarehouseId()),
                                    "sku", AttributeValue.fromS(item.getSku())
                            ))
                            .updateExpression("""
                                    SET quantity = quantity - :qty,
                                        reserved = if_not_exists(reserved, :zero) - :qty
                                    """)
                            .conditionExpression("quantity >= :qty AND reserved >= :qty")
                            .expressionAttributeValues(Map.of(
                                    ":qty", AttributeValue.fromN(String.valueOf(item.getQuantity())),
                                    ":zero", AttributeValue.fromN("0")
                            ))
                            .build())
                    .build());
        }

        ddbClient.transactWriteItems(
                TransactWriteItemsRequest.builder().transactItems(txItems).build());

        return ConsumeResult.CONSUMED;
    }

    @Override
    public void restockAtomically(String warehouseId, List<InventoryRestockTransaction.Item> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
        if (items.size() > 100) {
            throw new IllegalArgumentException("Max 100 items per Dynamo transaction");
        }

        String now = Instant.now().toString();

        List<TransactWriteItem> txItems = new ArrayList<>();

        for (InventoryRestockTransaction.Item item : items) {
            if (item.quantity() <= 0) throw new IllegalArgumentException("quantity must be > 0");
            if (item.sku() == null || item.sku().isBlank()) throw new IllegalArgumentException("sku must not be blank");

            txItems.add(TransactWriteItem.builder()
                    .update(Update.builder()
                            .tableName(inventoryTableName)
                            .key(Map.of(
                                    "warehouseId", AttributeValue.fromS(warehouseId),
                                    "sku", AttributeValue.fromS(item.sku())
                            ))
                            .updateExpression("""
                                SET quantity  = if_not_exists(quantity, :zero) + :qty,
                                    reserved  = if_not_exists(reserved, :zero),
                                    updatedAt = :now
                                """)
                            .expressionAttributeValues(Map.of(
                                    ":qty", AttributeValue.fromN(String.valueOf(item.quantity())),
                                    ":zero", AttributeValue.fromN("0"),
                                    ":now", AttributeValue.fromS(now)
                            ))
                            .build())
                    .build());
        }

        ddbClient.transactWriteItems(
                TransactWriteItemsRequest.builder().transactItems(txItems).build());
    }


    private static Map<String, AttributeValue> toReservationDdbItem(
            com.fulfillment.inventoryservice.domain.model.InventoryReservation r) {

        List<AttributeValue> itemsList = r.getItems().stream()
                .map(i -> AttributeValue.fromM(Map.of(
                        "sku", AttributeValue.fromS(i.sku()),
                        "quantity", AttributeValue.fromN(String.valueOf(i.quantity()))
                )))
                .toList();

        return Map.of(
                "reservationId", AttributeValue.fromS(r.getReservationId()),
                "orderId", AttributeValue.fromS(r.getOrderId()),
                "warehouseId", AttributeValue.fromS(r.getWarehouseId()),
                "items", AttributeValue.fromL(itemsList),
                "createdAtMs", AttributeValue.fromN(String.valueOf(r.getCreatedAt().toEpochMilli()))
        );
    }
}
