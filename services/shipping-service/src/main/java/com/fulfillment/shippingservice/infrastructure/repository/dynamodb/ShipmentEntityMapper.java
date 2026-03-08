package com.fulfillment.shippingservice.infrastructure.repository.dynamodb;

import java.util.List;

import com.fulfillment.shippingservice.domain.model.CarrierCode;
import com.fulfillment.shippingservice.domain.model.Shipment;
import com.fulfillment.shippingservice.domain.model.ShipmentItem;
import com.fulfillment.shippingservice.domain.model.ShipmentStatus;
import com.fulfillment.shippingservice.infrastructure.repository.dynamodb.shipment.ShipmentEntity;
import com.fulfillment.shippingservice.infrastructure.repository.dynamodb.shipment.ShipmentEntity.ShipmentItemEntry;

public final class ShipmentEntityMapper {

    private ShipmentEntityMapper() {
    }

    public static ShipmentEntity toEntity(Shipment shipment) {
        ShipmentEntity entity = new ShipmentEntity();
        entity.setShipmentId(shipment.getShipmentId());
        entity.setOrderId(shipment.getOrderId());
        entity.setWarehouseId(shipment.getWarehouseId());
        entity.setCarrier(shipment.getCarrier().name());
        entity.setStatus(shipment.getStatus().name());
        entity.setTrackingId(shipment.getTrackingId());
        entity.setCreatedAt(shipment.getCreatedAt());
        entity.setShippedAt(shipment.getShippedAt());
        entity.setEstimatedDeliveryAt(shipment.getEstimatedDeliveryAt());
        entity.setItems(toItemEntries(shipment.getItems())); 
        entity.setShippingGuideS3Key(shipment.getShippingGuideS3Key());       
        return entity;
    }

    public static Shipment toDomain(ShipmentEntity entity) {
        List<ShipmentItem> items = entity.getItems().stream()
                .map(entry -> ShipmentItem.createShipmentItem(entry.getSku(), entry.getQuantity()))
                .toList();

        return Shipment.restore(
                entity.getShipmentId(),
                entity.getOrderId(),
                entity.getWarehouseId(),
                CarrierCode.valueOf(entity.getCarrier()),
                ShipmentStatus.valueOf(entity.getStatus()),
                entity.getTrackingId(),
                items,
                entity.getCreatedAt(),
                entity.getShippedAt(),
                entity.getEstimatedDeliveryAt(),
                entity.getShippingGuideS3Key());
    }

    private static List<ShipmentItemEntry> toItemEntries(List<ShipmentItem> items) {
        return items.stream().map(item -> {
            ShipmentItemEntry entry = new ShipmentItemEntry();
            entry.setSku(item.getSku());
            entry.setQuantity(item.getQuantity());
            return entry;
        }).toList();
    }
}
