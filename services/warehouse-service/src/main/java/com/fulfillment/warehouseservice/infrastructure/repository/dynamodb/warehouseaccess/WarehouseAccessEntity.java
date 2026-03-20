package com.fulfillment.warehouseservice.infrastructure.repository.dynamodb.warehouseaccess;

import java.time.Instant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

@Getter
@Setter
@NoArgsConstructor
@DynamoDbBean
public class WarehouseAccessEntity {

    public static final String WAREHOUSE_ID_USER_ID_INDEX = "warehouseId-userId-index";

    private String userId;
    private String warehouseId;
    private boolean active;
    private Instant assignedAt;
    private String assignedBy;
    private Instant updatedAt;

    @DynamoDbPartitionKey
    @DynamoDbSecondarySortKey(indexNames = WAREHOUSE_ID_USER_ID_INDEX)
    public String getUserId() {
        return this.userId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = WAREHOUSE_ID_USER_ID_INDEX)
    public String getWarehouseId() {
        return this.warehouseId;
    }
}
