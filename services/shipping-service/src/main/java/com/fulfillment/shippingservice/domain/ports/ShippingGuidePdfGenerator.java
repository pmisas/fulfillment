package com.fulfillment.shippingservice.domain.ports;

import com.fulfillment.shippingservice.domain.model.Shipment;

public interface ShippingGuidePdfGenerator {
    byte[] generate(Shipment shipment);
}
