package com.fulfillment.shippingservice.infrastructure.repository.memory;

import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fulfillment.shippingservice.domain.model.Shipment;
import com.fulfillment.shippingservice.domain.model.ShipmentStatus;
import com.fulfillment.shippingservice.domain.ports.OutboxEventsRepository;
import com.fulfillment.shippingservice.domain.ports.OutboxEventsRepository.OutboxPendingEvent;
import com.fulfillment.shippingservice.domain.ports.ShipmentRepository;
import com.fulfillment.shippingservice.domain.ports.ShipmentWriteTransaction;

@Component
@Profile("local")
public class InMemoryShipmentWriteTransaction implements ShipmentWriteTransaction {

    private final ShipmentRepository shipmentRepository;
    private final OutboxEventsRepository outboxRepository;

    public InMemoryShipmentWriteTransaction(
            ShipmentRepository shipmentRepository,
            OutboxEventsRepository outboxRepository) {
        this.shipmentRepository = shipmentRepository;
        this.outboxRepository = outboxRepository;
    }

    @Override
    public Optional<Shipment> saveStatusWithOutbox(
            Shipment shipment,
            ShipmentStatus expectedCurrentStatus,
            OutboxPendingEvent event) {
        Optional<Shipment> saved = shipmentRepository.saveIfStatusMatches(shipment, expectedCurrentStatus);
        saved.ifPresent(s -> outboxRepository.savePendingIfAbsent(event));
        return saved;
    }
}
