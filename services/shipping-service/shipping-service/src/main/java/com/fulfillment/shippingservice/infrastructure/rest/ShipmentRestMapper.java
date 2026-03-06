package com.fulfillment.shippingservice.infrastructure.rest;

import com.fulfillment.shippingservice.application.dto.CreateShipmentCommand;
import com.fulfillment.shippingservice.domain.model.CarrierCode;
import com.fulfillment.shippingservice.domain.model.Shipment;
import com.fulfillment.shippingservice.infrastructure.rest.dto.CreateShipmentRequest;
import com.fulfillment.shippingservice.infrastructure.rest.dto.ShipmentResponse;

public final class ShipmentRestMapper {

    private ShipmentRestMapper() {
    }

    public static CreateShipmentCommand toCommand(CreateShipmentRequest request) {
        return new CreateShipmentCommand(
                request.orderId(),
                request.warehouseId(),
                toCarrierCode(request.carrier()),
                request.items().stream()
                        .map(item -> new CreateShipmentCommand.Item(item.sku(), item.quantity()))
                        .toList(),
                request.estimatedDeliveryAt());
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
                shipment.getEstimatedDeliveryAt());
    }

    private static CarrierCode toCarrierCode(String rawCarrier) {
        try {
            return CarrierCode.valueOf(rawCarrier.trim().toUpperCase().replace('-', '_'));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid carrier code: " + rawCarrier);
        }
    }
}
