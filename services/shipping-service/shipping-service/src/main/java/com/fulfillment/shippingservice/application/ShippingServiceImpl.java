package com.fulfillment.shippingservice.application;

import static com.fulfillment.shippingservice.domain.shared.DomainValidations.requireNonBlank;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.shippingservice.application.dto.CreateShipmentCommand;
import com.fulfillment.shippingservice.application.dto.ShipmentShippedPayload;
import com.fulfillment.shippingservice.domain.exception.ShipmentNotFoundException;
import com.fulfillment.shippingservice.domain.model.Shipment;
import com.fulfillment.shippingservice.domain.model.ShipmentItem;
import com.fulfillment.shippingservice.domain.model.ShipmentStatus;
import com.fulfillment.shippingservice.domain.ports.OutboxEventsRepository;
import com.fulfillment.shippingservice.domain.ports.OutboxEventsRepository.OutboxPendingEvent;
import com.fulfillment.shippingservice.domain.ports.ShipmentRepository;

@Service
public class ShippingServiceImpl implements ShippingService {

    private static final Logger log = LoggerFactory.getLogger(ShippingServiceImpl.class);

    private final ShipmentRepository shipmentRepository;
    private final OutboxEventsRepository outboxRepo;
    private final ObjectMapper mapper;

    public ShippingServiceImpl(
            ShipmentRepository shipmentRepository,
            OutboxEventsRepository outboxRepo,
            ObjectMapper mapper) {
        this.shipmentRepository = shipmentRepository;
        this.outboxRepo = outboxRepo;
        this.mapper = mapper;
    }

    @Override
    public Shipment create(CreateShipmentCommand command) {
        List<Shipment> existing = shipmentRepository.findByOrderId(command.orderId());
        if (!existing.isEmpty()) {
            log.info("Shipment already exists for orderId={}, returning existing (idempotent)", command.orderId());
            return existing.get(0);
        }

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

        Shipment saved = shipmentRepository.save(current);

        String eventId = "ShipmentShipped:" + saved.getShipmentId();
        OutboxPendingEvent event = new OutboxPendingEvent(
                eventId,
                "SHIPMENT",
                saved.getShipmentId(),
                "ShipmentShipped",
                buildShipmentShippedPayload(saved));

        boolean inserted = outboxRepo.savePendingIfAbsent(event);
        if (!inserted) {
            outboxRepo.resetToPendingIfProcessed(eventId);
        }
        log.info("ShipmentShipped outbox event queued for shipmentId={} orderId={}", saved.getShipmentId(), saved.getOrderId());

        return saved;
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

    private String buildShipmentShippedPayload(Shipment shipment) {
        try {
            return mapper.writeValueAsString(
                    new ShipmentShippedPayload(shipment.getOrderId(), shipment.getShipmentId(), shipment.getTrackingId()));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize ShipmentShipped payload: " + e.getMessage(), e);
        }
    }
}
