package com.fulfillment.notificationlambda.infrastructure.order;

import com.fulfillment.notificationlambda.domain.model.OrderInfo;
import com.fulfillment.notificationlambda.domain.ports.OrderLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DynamoOrderLookup implements OrderLookup {

    private static final Logger log = LoggerFactory.getLogger(DynamoOrderLookup.class);

    private final DynamoDbClient dynamo;
    private final String tableName;

    public DynamoOrderLookup(DynamoDbClient dynamo, String tableName) {
        this.dynamo = dynamo;
        this.tableName = tableName;
    }

    @Override
    public Optional<OrderInfo> findById(String orderId) {
        GetItemRequest request = GetItemRequest.builder()
            .tableName(tableName)
            .key(Map.of("orderId", AttributeValue.fromS(orderId)))
            .build();

        GetItemResponse response = dynamo.getItem(request);

        if (!response.hasItem() || response.item().isEmpty()) {
            log.warn("Order {} not found in table {}", orderId, tableName);
            return Optional.empty();
        }

        Map<String, AttributeValue> item = response.item();
        String operatorId = getString(item, "operatorId");

        List<OrderInfo.OrderItem> items = new ArrayList<>();
        AttributeValue itemsAttr = item.get("items");
        if (itemsAttr != null && itemsAttr.hasL()) {
            for (AttributeValue entry : itemsAttr.l()) {
                if (!entry.hasM()) continue;
                Map<String, AttributeValue> m = entry.m();
                String sku = getString(m, "sku");
                String quantityStr = getNumber(m, "quantity");
                int qty = quantityStr != null ? Integer.parseInt(quantityStr) : 0;
                if (sku != null) items.add(new OrderInfo.OrderItem(sku, qty));
            }
        }

        return Optional.of(new OrderInfo(orderId, operatorId, items));
    }

    private static String getString(Map<String, AttributeValue> map, String key) {
        AttributeValue v = map.get(key);
        return (v != null && v.s() != null) ? v.s() : null;
    }

    private static String getNumber(Map<String, AttributeValue> map, String key) {
        AttributeValue v = map.get(key);
        return (v != null && v.n() != null) ? v.n() : null;
    }
}
