package com.fulfillment.inventoryservice.infraestrcture.repository.dynamodb;

import java.time.Instant;

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
public class InventoryItemEntity {

    private String warehouseId;  
    private String sku;         

    private Integer quantity;
    private Integer reserved;
    private Instant updatedAt;

    private String lowStockKey;
    private Integer available; 

    @DynamoDbPartitionKey
    public String getWarehouseId() { 
        return warehouseId; 
    }

    @DynamoDbSortKey
    public String getSku() { 
        return sku; 
    }

}
