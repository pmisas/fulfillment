package com.fulfillment.orderservice.infrastructure.repository.dynamodb.history;

import java.time.Instant;

import com.fulfillment.orderservice.domain.model.Status;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Getter
@Setter
@NoArgsConstructor
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

    @DynamoDbSortKey
    public Instant getChangedAt() { 
        return changedAt; 
    }

}
