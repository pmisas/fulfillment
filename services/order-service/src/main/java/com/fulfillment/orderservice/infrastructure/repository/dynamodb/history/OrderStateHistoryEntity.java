package com.fulfillment.orderservice.infrastructure.repository.dynamodb.history;

import java.time.Instant;

import com.fulfillment.orderservice.domain.model.Status;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class OrderStateHistoryEntity {

    private String orderId;    
    private Instant changedAt;
    private String historyId; 
    private Status fromStatus;
    private Status toStatus;

    @DynamoDbPartitionKey
    public String getOrderId() { 
        return orderId; 
    }

    public void setOrderId(String orderId) { 
        this.orderId = orderId; 
    }

    @DynamoDbSortKey
    public Instant getChangedAt() { 
        return changedAt; 
    }

    public void setChangedAt(Instant changedAt) { 
        this.changedAt = changedAt; 
    }

    public String getHistoryId() { 
        return historyId; 
    }

    public void setHistoryId(String historyId) { 
        this.historyId = historyId; 
    }

    public Status getFromStatus() { 
        return fromStatus; 
    }

    public void setFromStatus(Status fromStatus) { 
        this.fromStatus = fromStatus; 
    }

    public Status getToStatus() { 
        return toStatus; 
    }

    public void setToStatus(Status toStatus) { 
        this.toStatus = toStatus; 
    }
}
