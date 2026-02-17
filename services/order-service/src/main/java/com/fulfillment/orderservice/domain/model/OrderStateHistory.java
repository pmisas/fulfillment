package com.fulfillment.orderservice.domain.model;

import static com.fulfillment.orderservice.domain.shared.DomainValidations.requireNonBlank;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class OrderStateHistory {
    
    private final String orderId;
    private final String historyId;
    private final Status fromStatus;
    private final Status toStatus;
    private final Instant changedAt;


    private OrderStateHistory(
            String historyId,
            String orderId,
            Status fromStatus,
            Status toStatus,
            Instant changedAt
    ) {
        this.historyId = requireNonBlank(historyId, "historyId");
        this.orderId = requireNonBlank(orderId, "orderId");
        this.fromStatus = Objects.requireNonNull(fromStatus, "fromStatus");
        this.toStatus = Objects.requireNonNull(toStatus, "toStatus");
        this.changedAt = Objects.requireNonNull(changedAt, "changedAt");
    }

    public static OrderStateHistory createOrderStateHistory(
                    String orderId,
                    Status fromStatus, 
                    Status toStatus
        ) {
        Instant now = Instant.now();
        return new OrderStateHistory(
            UUID.randomUUID().toString(),
            orderId,
            fromStatus,
            toStatus,
            now
        );
    }

     public static OrderStateHistory restore(
                String historyId,
                String orderId,
                Status fromStatus, 
                Status toStatus,
                Instant changedAt
    ) {
        return new OrderStateHistory(historyId, orderId, fromStatus, toStatus, changedAt);
    }

    public String getHistoryId() {
        return historyId;
    }

    public String getOrderId() {
        return orderId;
    }

    public Status getFromStatus() {
        return fromStatus;
    }

    public Status getToStatus() {
        return toStatus;
    }

    public Instant getchangedAt() {
        return changedAt;
    }
}
