package com.fulfillment.inventoryservice.infraestrcture.repository.dynamodb.inventoryReservation;

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
public class InventoryReservationEntity {
    private String reservationId;
    private String orderId;
    private String warehouseId;
    private Long createdAtMs;
    private Long ttl;

    private List<Item> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @DynamoDbBean
    public static class Item {
        private String sku;
        private Integer quantity;
    }

    @DynamoDbPartitionKey
    public String getReservationId() { return reservationId; }

}
