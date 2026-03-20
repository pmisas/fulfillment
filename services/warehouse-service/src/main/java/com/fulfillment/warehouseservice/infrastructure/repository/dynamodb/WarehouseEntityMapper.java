package com.fulfillment.warehouseservice.infrastructure.repository.dynamodb;

import java.time.Duration;
import java.time.Instant;

import com.fulfillment.warehouseservice.domain.model.WarehouseAccess;
import com.fulfillment.warehouseservice.domain.model.Warehouse;
import com.fulfillment.warehouseservice.domain.port.OutboxEventsRepository.OutboxPendingEvent;
import com.fulfillment.warehouseservice.infrastructure.repository.dynamodb.outbox.OutboxEventEntity;
import com.fulfillment.warehouseservice.infrastructure.repository.dynamodb.warehouseaccess.WarehouseAccessEntity;
import com.fulfillment.warehouseservice.infrastructure.repository.dynamodb.warehouse.WarehouseEntity;

public final class WarehouseEntityMapper {

    private static final Duration OUTBOX_TTL = Duration.ofDays(7);

    private WarehouseEntityMapper() {}

    public static WarehouseEntity toEntity(Warehouse warehouse) {
        WarehouseEntity e = new WarehouseEntity();
        e.setWarehouseId(warehouse.getWarehouseId());
        e.setCity(warehouse.getCity());
        e.setLat(warehouse.getLat());
        e.setLng(warehouse.getLng());
        e.setCreatedAt(warehouse.getCreatedAt());

        return e;
    }

    public static Warehouse toDomain(WarehouseEntity e) {
        return Warehouse.restore(
            e.getWarehouseId(),
            e.getCity(),
            e.getLat(),
            e.getLng(),
            e.getCreatedAt()
        );
    }

    public static WarehouseAccessEntity toEntity(WarehouseAccess access) {
        WarehouseAccessEntity entity = new WarehouseAccessEntity();
        entity.setUserId(access.getUserId());
        entity.setWarehouseId(access.getWarehouseId());
        entity.setActive(access.isActive());
        entity.setAssignedAt(access.getAssignedAt());
        entity.setAssignedBy(access.getAssignedBy());
        entity.setUpdatedAt(access.getUpdatedAt());
        return entity;
    }

    public static WarehouseAccess toDomain(WarehouseAccessEntity entity) {
        return WarehouseAccess.restore(
            entity.getUserId(),
            entity.getWarehouseId(),
            entity.isActive(),
            entity.getAssignedAt(),
            entity.getAssignedBy(),
            entity.getUpdatedAt()
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

}
