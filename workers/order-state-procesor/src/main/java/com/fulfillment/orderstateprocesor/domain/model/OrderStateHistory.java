package com.fulfillment.orderstateprocesor.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static com.fulfillment.orderstateprocesor.domain.shared.DomainValidations.requireNonBlank;

public class OrderStateHistory {
    private final String historyId;
    private final String orderId;
    private final Status fromStatus;
    private final Status toStatus;
    private final Instant changedAt;

    private OrderStateHistory(String historyId, String orderId, Status fromStatus, Status toStatus, Instant changedAt) {
        this.historyId = requireNonBlank(historyId, "historyId");
        this.orderId = requireNonBlank(orderId, "orderId");
        this.fromStatus = fromStatus; // puede ser null para el primer estado
        this.toStatus = Objects.requireNonNull(toStatus, "toStatus");
        this.changedAt = Objects.requireNonNull(changedAt, "changedAt");
    }

    public static OrderStateHistory initial(String orderId, Status toStatus) {
        return new OrderStateHistory(UUID.randomUUID().toString(), orderId, null, toStatus, Instant.now());
    }

    public static OrderStateHistory transition(String orderId, Status from, Status to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from == to) throw new IllegalArgumentException("fromStatus and toStatus must be different");
        return new OrderStateHistory(UUID.randomUUID().toString(), orderId, from, to, Instant.now());
    }

    public String getHistoryId() { return historyId; }
    public String getOrderId() { return orderId; }
    public Status getFromStatus() { return fromStatus; }
    public Status getToStatus() { return toStatus; }
    public Instant getChangedAt() { return changedAt; }
} 
