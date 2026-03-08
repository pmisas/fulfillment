package com.fulfillment.shippingservice.infrastructure.repository.dynamodb.shipment;

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
public class ShipmentEntity {

    private String shipmentId;
    private String orderId;
    private String warehouseId;
    private String carrier;
    private String status;
    private String trackingId;
    private List<ShipmentItemEntry> items;
    private Instant createdAt;
    private Instant shippedAt;
    private Instant estimatedDeliveryAt;
    private String shippingGuideS3Key;

    @DynamoDbPartitionKey
    public String getShipmentId() {
        return shipmentId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @DynamoDbBean
    public static class ShipmentItemEntry {
        private String sku;
        private int quantity;
    }
}
