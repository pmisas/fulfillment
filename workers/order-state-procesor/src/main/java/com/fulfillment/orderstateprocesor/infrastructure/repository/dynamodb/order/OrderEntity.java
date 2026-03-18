package com.fulfillment.orderstateprocesor.infrastructure.repository.dynamodb.order;

import java.time.Instant;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Getter
@Setter
@NoArgsConstructor
@DynamoDbBean
public class OrderEntity {

    private String orderId;
    private String operatorId;
    private String warehouseId;
    private String status; 
    private Instant createdAt;
    private Instant updatedAt;
    private Double lat;
    private Double lng;
    private List<Item> items;

    @DynamoDbPartitionKey
    public String getOrderId() { 
        return orderId; 
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @DynamoDbBean
    public static class Item {
        private String sku;
        private Integer quantity;

    }
}
