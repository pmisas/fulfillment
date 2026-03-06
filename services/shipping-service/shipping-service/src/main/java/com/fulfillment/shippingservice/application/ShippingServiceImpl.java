package com.fulfillment.shippingservice.application;

import static com.fulfillment.shippingservice.domain.shared.DomainValidations.requireNonBlank;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fulfillment.shippingservice.application.dto.CreateShipmentCommand;
import com.fulfillment.shippingservice.domain.exception.ShipmentNotFoundException;
import com.fulfillment.shippingservice.domain.model.Shipment;
import com.fulfillment.shippingservice.domain.model.ShipmentItem;
import com.fulfillment.shippingservice.domain.model.ShipmentStatus;
import com.fulfillment.shippingservice.domain.ports.ShipmentRepository;

@Service
public class ShippingServiceImpl implements ShippingService {

    private final ShipmentRepository shipmentRepository;

    public ShippingServiceImpl(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }

    @Override
    public Shipment create(CreateShipmentCommand command) {
        String shipmentId = UUID.randomUUID().toString();

        List<ShipmentItem> items = command.items().stream()
                .map(item -> ShipmentItem.createShipmentItem(item.sku(), item.quantity()))
                .toList();

        Shipment shipment = Shipment.createShipment(
                shipmentId,
                command.orderId(),
                command.warehouseId(),
                command.carrier(),
                items,
                command.estimatedDeliveryAt());

        return shipmentRepository.save(shipment);
    }

    @Override
    public Shipment getById(String shipmentId) {
        String normalizedId = requireNonBlank(shipmentId, "shipmentId").trim();
        return shipmentRepository.findById(normalizedId)
                .orElseThrow(() -> new ShipmentNotFoundException(normalizedId));
    }

    @Override
    public List<Shipment> getAll() {
        return shipmentRepository.findAll();
    }

    @Override
    public List<Shipment> getByOrderId(String orderId) {
        String normalizedOrderId = requireNonBlank(orderId, "orderId").trim();
        return shipmentRepository.findByOrderId(normalizedOrderId);
    }

    @Override
    public Shipment markAsShipped(String shipmentId, String trackingId) {
        Shipment current = getById(shipmentId)
                .withTrackingId(trackingId)
                .withStatus(ShipmentStatus.SHIPPED);

        return shipmentRepository.save(current);
    }

    @Override
    public Shipment markInTransit(String shipmentId) {
        return transitionStatus(shipmentId, ShipmentStatus.IN_TRANSIT);
    }

    @Override
    public Shipment markAsDelivered(String shipmentId) {
        return transitionStatus(shipmentId, ShipmentStatus.DELIVERED);
    }

    private Shipment transitionStatus(String shipmentId, ShipmentStatus targetStatus) {
        Shipment updated = getById(shipmentId).withStatus(targetStatus);
        return shipmentRepository.save(updated);
    }
}
