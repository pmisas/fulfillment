package com.fulfillment.shippingservice.application;

import java.util.List;
import java.time.Instant;

import com.fulfillment.shippingservice.domain.model.CarrierCode;
import com.fulfillment.shippingservice.domain.model.Shipment;

public interface ShippingService {

    Shipment create(
        String orderId,
        String warehouseId,
        CarrierCode carrier,
        List<ShipmentItemInput> items,
        Instant estimatedDeliveryAt
    );
    Shipment getById(String shipmentId);
    List<Shipment> getAll();
    List<Shipment> getByOrderId(String orderId);
    Shipment markAsShipped(String shipmentId);
    Shipment markAsDelivered(String shipmentId);
    String getShippingGuideUrl(String shipmentId);

    record ShipmentItemInput(String sku, int quantity) {}
}
