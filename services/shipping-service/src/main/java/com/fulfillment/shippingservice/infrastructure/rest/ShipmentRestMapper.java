package com.fulfillment.shippingservice.infrastructure.rest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.fulfillment.shippingservice.application.dto.CreateShipmentCommand;
import com.fulfillment.shippingservice.domain.model.CarrierCode;
import com.fulfillment.shippingservice.domain.model.Shipment;
import com.fulfillment.shippingservice.infrastructure.rest.dto.InternalCreateShipmentRequest;
import com.fulfillment.shippingservice.infrastructure.rest.dto.ShipmentResponse;

public final class ShipmentRestMapper {

    private ShipmentRestMapper() {
    }

    public static CreateShipmentCommand toCommand(InternalCreateShipmentRequest request) {
        return new CreateShipmentCommand(
                request.orderId(),
                request.warehouseId(),
                CarrierCode.INTERNAL_CARRIER,
                request.items().stream()
                        .map(item -> new CreateShipmentCommand.Item(item.sku(), item.quantity()))
                        .toList(),
                Instant.now().plus(7, ChronoUnit.DAYS));
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