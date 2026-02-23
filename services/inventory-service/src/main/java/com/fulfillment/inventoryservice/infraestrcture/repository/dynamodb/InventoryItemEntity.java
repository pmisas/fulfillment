package com.fulfillment.inventoryservice.infraestrcture.repository.dynamodb;

import java.time.Instant;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

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

    public void setWarehouseId(String warehouseId) { 
        this.warehouseId = warehouseId; 
    }

    @DynamoDbSortKey
    public String getSku() { 
        return sku; 
    }

    public void setSku(String sku) { 
        this.sku = sku; 
    }

    public Integer getQuantity() { 
        return quantity; 
    }

    public void setQuantity(Integer quantity) { 
        this.quantity = quantity; 
    }

    public Integer getReserved() { 
        return reserved; 
    }

    public void setReserved(Integer reserved) { 
        this.reserved = reserved; 
    }

    public Instant getUpdatedAt() { 
        return updatedAt; 
    }

    public void setUpdatedAt(Instant updatedAt) { 
        this.updatedAt = updatedAt; 
    }

    public String getLowStockKey() { 
        return lowStockKey; 
    }

    public void setLowStockKey(String lowStockKey) { 
        this.lowStockKey = lowStockKey; 
    }

    public Integer getAvailable() { 
        return available; 
    }

    public void setAvailable(Integer available) { 
        this.available = available; 
    }
}
