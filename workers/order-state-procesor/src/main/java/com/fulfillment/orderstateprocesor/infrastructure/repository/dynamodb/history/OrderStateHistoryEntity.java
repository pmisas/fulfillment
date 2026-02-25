package com.fulfillment.orderstateprocesor.infrastructure.repository.dynamodb.history;

import java.time.Instant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Getter
@Setter
@NoArgsConstructor
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

}
