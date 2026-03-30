package com.fulfillment.shippingservice.infrastructure.rest;

import com.fulfillment.shippingservice.domain.model.Shipment;
import com.fulfillment.shippingservice.infrastructure.rest.dto.response.ShipmentResponse;

public final class ShipmentRestMapper {

    private ShipmentRestMapper() {
    }

    public static ShipmentResponse toResponse(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getShipmentId(),
                shipment.getOrderId(),
                shipment.getWarehouseId(),
                shipment.getCarrier().name(),
                shipment.getStatus().name(),
                shipment.getTrackingId(),
                shipment.getItems().stream()
                        .map(item -> new ShipmentResponse.Item(item.getSku(), item.getQuantity()))
                        .toList(),
                shipment.getCreatedAt(),
                shipment.getShippedAt(),
                shipment.getEstimatedDeliveryAt(),
                shipment.getShippingGuideS3Key());
    }
}
