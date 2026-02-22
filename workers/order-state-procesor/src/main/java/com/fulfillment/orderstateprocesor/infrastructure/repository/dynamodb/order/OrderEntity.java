package com.fulfillment.orderstateprocesor.infrastructure.repository.dynamodb.order;

import java.time.Instant;
import java.util.List;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class OrderEntity {

    private String orderId;
    private String customerId;
    private String warehouseId;
    private String status; 
    private Instant createdAt;
    private Instant updatedAt;
    private List<Item> items;

    @DynamoDbPartitionKey
    public String getOrderId() { 
        return orderId; 
    }

    public void setOrderId(String orderId) { 
        this.orderId = orderId; 
    }

    public String getCustomerId() { 
        return customerId; 
    }

    public void setCustomerId(String customerId) { 
        this.customerId = customerId; 
    }

    public String getWarehouseId() { 
        return warehouseId; 
    }

    public void setWarehouseId(String warehouseId) { 
        this.warehouseId = warehouseId; 
    }

    public String getStatus() { 
        return status; 
    }

    public void setStatus(String status) { 
        this.status = status; 
    }

    public Instant getCreatedAt() { 
        return createdAt; 
    }

    public void setCreatedAt(Instant createdAt) { 
        this.createdAt = createdAt; 
    }

    public Instant getUpdatedAt() { 
        return updatedAt; 
    }

    public void setUpdatedAt(Instant updatedAt) { 
        this.updatedAt = updatedAt; 
    }

    public List<Item> getItems() { 
        return items; 
    }

    public void setItems(List<Item> items) { 
        this.items = items; 
    }

    @DynamoDbBean
    public static class Item {
        private String sku;
        private Integer quantity;

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
    }
}
