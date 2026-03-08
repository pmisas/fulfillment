package com.fulfillment.shippingservice.domain.exception;

public class ShipmentNotFoundException extends RuntimeException {

    public ShipmentNotFoundException(String shipmentId) {
        super("Shipment not found: " + shipmentId);
    }
}
