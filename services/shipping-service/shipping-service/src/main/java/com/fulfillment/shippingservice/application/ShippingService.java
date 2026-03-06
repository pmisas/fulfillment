package com.fulfillment.shippingservice.application;

import java.util.List;

import com.fulfillment.shippingservice.application.dto.CreateShipmentCommand;
import com.fulfillment.shippingservice.domain.model.Shipment;

public interface ShippingService {

    Shipment create(CreateShipmentCommand command);

    Shipment getById(String shipmentId);

    List<Shipment> getAll();

    List<Shipment> getByOrderId(String orderId);

    Shipment markAsShipped(String shipmentId, String trackingId);

    Shipment markInTransit(String shipmentId);

    Shipment markAsDelivered(String shipmentId);

}
