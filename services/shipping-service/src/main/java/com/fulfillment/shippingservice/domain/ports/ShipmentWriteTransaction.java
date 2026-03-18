package com.fulfillment.shippingservice.domain.ports;

import java.util.Optional;

import com.fulfillment.shippingservice.domain.model.Shipment;
import com.fulfillment.shippingservice.domain.model.ShipmentStatus;
import com.fulfillment.shippingservice.domain.ports.OutboxEventsRepository.OutboxPendingEvent;

public interface ShipmentWriteTransaction {

    Optional<Shipment> saveStatusWithOutbox(
        Shipment shipment,
        ShipmentStatus expectedCurrentStatus,
        OutboxPendingEvent event
    );
}
