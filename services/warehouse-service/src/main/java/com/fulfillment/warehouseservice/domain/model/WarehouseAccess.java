package com.fulfillment.warehouseservice.domain.model;

import java.time.Instant;

import static com.fulfillment.warehouseservice.domain.shared.DomainValidations.requireNonBlank;

import lombok.Getter;

@Getter
public class WarehouseAccess {

    private final String userId;
    private final String warehouseId;
    private final boolean active;
    private final Instant assignedAt;
    private final String assignedBy;
    private final Instant updatedAt;

    private WarehouseAccess(
            String userId,
            String warehouseId,
            boolean active,
            Instant assignedAt,
            String assignedBy,
            Instant updatedAt) {
        this.userId = userId;
        this.warehouseId = warehouseId;
        this.active = active;
        this.assignedAt = assignedAt;
        this.assignedBy = assignedBy;
        this.updatedAt = updatedAt;
    }

    public static WarehouseAccess assign(String userId, String warehouseId, String assignedBy, Instant now) {
        Instant timestamp = now == null ? Instant.now() : now;
        return new WarehouseAccess(
            requireNonBlank(userId, "userId").trim(),
            requireNonBlank(warehouseId, "warehouseId").trim(),
            true,
            timestamp,
            normalizeOptional(assignedBy),
            timestamp
        );
    }

    public static WarehouseAccess restore(
            String userId,
            String warehouseId,
            boolean active,
            Instant assignedAt,
            String assignedBy,
            Instant updatedAt) {
        return new WarehouseAccess(
            requireNonBlank(userId, "userId").trim(),
            requireNonBlank(warehouseId, "warehouseId").trim(),
            active,
            assignedAt,
            normalizeOptional(assignedBy),
            updatedAt
        );
    }

    public WarehouseAccess deactivate(Instant now) {
        return new WarehouseAccess(
            userId,
            warehouseId,
            false,
            assignedAt,
            assignedBy,
            now == null ? Instant.now() : now
        );
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public boolean isActive() {
        return active;
    }

}
