package com.fulfillment.inventoryservice.infraestrcture.repository.dynamodb.warehouseaccess;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Getter
@Setter
@NoArgsConstructor
@DynamoDbBean
public class WarehouseAccessEntity {

    private String userId;
    private String warehouseId;
    private boolean active;

    @DynamoDbPartitionKey
    public String getUserId() {
        return userId;
    }
}
