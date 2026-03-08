package com.fulfillment.shippingservice.infrastructure.repository.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.fulfillment.shippingservice.domain.model.Shipment;
import com.fulfillment.shippingservice.domain.model.ShipmentStatus;
import com.fulfillment.shippingservice.domain.ports.ShipmentRepository;

@Repository
@Profile("local")
public class InMemoryShipmentRepositoryAdapter implements ShipmentRepository {

    private final ConcurrentMap<String, Shipment> db = new ConcurrentHashMap<>();

    @Override
    public Shipment save(Shipment shipment) {
        db.put(shipment.getShipmentId(), shipment);
        return shipment;
    }

    @Override
    public Optional<Shipment> saveIfStatusMatches(Shipment shipment, ShipmentStatus expectedCurrentStatus) {
        boolean[] saved = { false };
        db.compute(shipment.getShipmentId(), (id, existing) -> {
            if (existing != null && existing.getStatus() == expectedCurrentStatus) {
                saved[0] = true;
                return shipment;
            }
            return existing;
        });
        return saved[0] ? Optional.of(shipment) : Optional.empty();
    }

    @Override
    public Optional<Shipment> findById(String shipmentId) {
        return Optional.ofNullable(db.get(shipmentId));
    }

    @Override
    public List<Shipment> findAll() {
        return new ArrayList<>(db.values());
    }

    @Override
    public List<Shipment> findByOrderId(String orderId) {
        return db.values().stream()
                .filter(shipment -> shipment.getOrderId().equals(orderId))
                .toList();
    }
}
