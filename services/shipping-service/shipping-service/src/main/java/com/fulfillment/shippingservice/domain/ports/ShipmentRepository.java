package com.fulfillment.shippingservice.domain.ports;

import java.util.List;
import java.util.Optional;

import com.fulfillment.shippingservice.domain.model.Shipment;

public interface ShipmentRepository {

    Shipment save(Shipment shipment);

    Optional<Shipment> findById(String shipmentId);

    List<Shipment> findAll();

    List<Shipment> findByOrderId(String orderId);
}
