package com.fulfillment.warehouseservice.infrastructure.repository.dynamodb;

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
public class WarehouseEntity {
    
    private String warehouseId;
    private String city;
    private double lat;
    private double lng;
    private Instant createdAt;

    @DynamoDbPartitionKey
    public String getWarehouseId() {
        return this.warehouseId;
    }

}
