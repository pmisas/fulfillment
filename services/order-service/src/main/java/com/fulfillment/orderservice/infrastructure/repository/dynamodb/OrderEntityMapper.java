package com.fulfillment.orderservice.infrastructure.repository.dynamodb;

import com.fulfillment.orderservice.domain.model.Order;
import com.fulfillment.orderservice.domain.model.OrderItem;
import com.fulfillment.orderservice.domain.model.OrderStateHistory;
import com.fulfillment.orderservice.domain.model.Status;
import com.fulfillment.orderservice.domain.ports.OrderWriteTransaction.OutboxPendingEvent;
import com.fulfillment.orderservice.infrastructure.repository.dynamodb.history.OrderStateHistoryEntity;
import com.fulfillment.orderservice.infrastructure.repository.dynamodb.order.OrderEntity;
import com.fulfillment.orderservice.infrastructure.repository.dynamodb.outbox.OutboxEventEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class OrderEntityMapper {

    private static final Duration OUTBOX_TTL = Duration.ofDays(7);

    private OrderEntityMapper() {}

    public static OrderEntity toEntity(Order order) {
        OrderEntity e = new OrderEntity();
        e.setOrderId(order.getOrderId());
        e.setWarehouseId(order.getWarehouseId());
        e.setStatus(order.getStatus().name());
        e.setCreatedAt(order.getCreatedAt());
        e.setUpdatedAt(order.getUpdatedAt());
        e.setLat(order.getLat());
        e.setLng(order.getLng());
        e.setItems(toItemEntities(order.getItems()));
        return e;
    }

    public static Order toDomain(OrderEntity e) {
        List<OrderItem> items = e.getItems().stream()
                .map(i -> OrderItem.createOrderItem(i.getSku(), i.getQuantity()))
                .toList();

        return Order.restore(
                e.getOrderId(),
                e.getWarehouseId(),
                Status.valueOf(e.getStatus()),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getLat(),
                e.getLng(),
                items
        );
    }


    public static OrderStateHistoryEntity toEntity(OrderStateHistory history) {
        OrderStateHistoryEntity e = new OrderStateHistoryEntity();
        e.setOrderId(history.getOrderId());
        e.setHistoryId(history.getHistoryId());
        e.setChangedAt(history.getChangedAt());
        e.setFromStatus(history.getFromStatus());
        e.setToStatus(history.getToStatus());
        return e;
    }

    public static OrderStateHistory toDomain(OrderStateHistoryEntity e) {
        return OrderStateHistory.restore(
                e.getHistoryId(),
                e.getOrderId(),
                e.getFromStatus(),
                e.getToStatus(),
                e.getChangedAt()
        );
    }

    public static OutboxEventEntity toEntity(OutboxPendingEvent evt) {
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

    private static List<OrderEntity.Item> toItemEntities(List<OrderItem> items) {
        return items.stream().map(i -> {
            OrderEntity.Item e = new OrderEntity.Item();
            e.setSku(i.getSku());
            e.setQuantity(i.getQuantity());
            return e;
        }).toList();
    }
}
