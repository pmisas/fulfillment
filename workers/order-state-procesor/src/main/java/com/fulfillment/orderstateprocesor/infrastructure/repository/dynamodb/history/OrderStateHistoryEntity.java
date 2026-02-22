package com.fulfillment.orderstateprocesor.infrastructure.repository.dynamodb.history;

import java.time.Instant;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class OrderStateHistoryEntity {

    private String historyId;
    private String orderId;
    private String fromStatus;
    private String toStatus;
    private Instant changedAt;

    @DynamoDbPartitionKey
    public String getHistoryId() { 
        return historyId; 
    }

    public void setHistoryId(String historyId) { 
        this.historyId = historyId; 
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { 
        this.orderId = orderId; 
    }

    public String getFromStatus() { 
        return fromStatus; 
    }

    public void setFromStatus(String fromStatus) { 
        this.fromStatus = fromStatus; 
    }

    public String getToStatus() { 
        return toStatus; 
    }

    public void setToStatus(String toStatus) { 
        this.toStatus = toStatus; 
    }

    public Instant getChangedAt() { 
        return changedAt; 
    }

    public void setChangedAt(Instant changedAt) { 
        this.changedAt = changedAt; 
    }
}
