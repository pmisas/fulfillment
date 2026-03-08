package com.fulfillment.shippingservice.domain.exception;

public class ShipmentGuideNotReadyException extends RuntimeException {

    public ShipmentGuideNotReadyException(String shipmentId) {
        super("Shipping guide not yet available for shipment '" + shipmentId
                + "'. The shipment must be in SHIPPED status and the guide must have been generated.");
    }
}
