package com.fulfillment.shippingservice.application;

import static com.fulfillment.shippingservice.domain.shared.DomainValidations.requireNonBlank;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.shippingservice.application.dto.CreateShipmentCommand;
import com.fulfillment.shippingservice.application.dto.ShipmentShippedPayload;
import com.fulfillment.shippingservice.application.dto.ShipmentDeliveredPayload;
import com.fulfillment.shippingservice.domain.exception.InvalidStatusTransitionException;
import com.fulfillment.shippingservice.domain.exception.ShipmentGuideNotReadyException;
import com.fulfillment.shippingservice.domain.exception.ShipmentNotFoundException;
import com.fulfillment.shippingservice.domain.model.Shipment;
import com.fulfillment.shippingservice.domain.model.ShipmentItem;
import com.fulfillment.shippingservice.domain.model.ShipmentStatus;
import com.fulfillment.shippingservice.domain.ports.OutboxEventsRepository.OutboxPendingEvent;
import com.fulfillment.shippingservice.domain.ports.ShipmentRepository;
import com.fulfillment.shippingservice.domain.ports.ShipmentWriteTransaction;
import com.fulfillment.shippingservice.domain.ports.ShippingGuidePdfGenerator;
import com.fulfillment.shippingservice.domain.ports.ShippingGuideStorage;

@Service
public class ShippingServiceImpl implements ShippingService {

    private static final Logger log = LoggerFactory.getLogger(ShippingServiceImpl.class);

    private final ShipmentRepository shipmentRepository;
    private final ShipmentWriteTransaction shipmentWriteTx;
    private final ObjectMapper mapper;
    private final ShippingGuidePdfGenerator pdfGenerator;
    private final ShippingGuideStorage shippingGuideStorage;

    public ShippingServiceImpl(
            ShipmentRepository shipmentRepository,
            ShipmentWriteTransaction shipmentWriteTx,
            ObjectMapper mapper,
            ShippingGuidePdfGenerator pdfGenerator,
            ShippingGuideStorage shippingGuideStorage) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentWriteTx = shipmentWriteTx;
        this.mapper = mapper;
        this.pdfGenerator = pdfGenerator;
        this.shippingGuideStorage = shippingGuideStorage;
    }

    @Override
    public Shipment create(CreateShipmentCommand command) {
        List<Shipment> existing = shipmentRepository.findByOrderId(command.orderId());
        if (!existing.isEmpty()) {
            Shipment existingShipment = existing.get(0);
            if (existingShipment.getShippingGuideS3Key() != null) {
                log.info("Shipment already exists for orderId={}, returning existing (idempotent)", command.orderId());
                return existingShipment;
            }
            log.info("Shipment exists for orderId={} but guide not uploaded, regenerating", command.orderId());
            byte[] pdfBytes = pdfGenerator.generate(existingShipment);
            String s3Key = shippingGuideStorage.upload(existingShipment.getShipmentId(), pdfBytes);
            return shipmentRepository.save(existingShipment.withShippingGuideS3Key(s3Key));
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

        Shipment saved = shipmentRepository.save(shipment);

        byte[] pdfBytes = pdfGenerator.generate(saved);
        String s3Key = shippingGuideStorage.upload(saved.getShipmentId(), pdfBytes);
        Shipment withGuide = shipmentRepository.save(saved.withShippingGuideS3Key(s3Key));

        log.info("Shipment created with guide for orderId={} shipmentId={}",
                command.orderId(), shipmentId);
        return withGuide;
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
    public Shipment markAsShipped(String shipmentId) {
        String normalizedShipmentId = requireNonBlank(shipmentId, "shipmentId").trim();

        Shipment current = getById(normalizedShipmentId);

        if (current.getStatus() == ShipmentStatus.SHIPPED) {
            log.info("Shipment already SHIPPED (idempotent), returning existing shipmentId={}", normalizedShipmentId);
            return current;
        }

        Shipment candidate = current.withStatus(ShipmentStatus.SHIPPED);

        OutboxPendingEvent outboxEvent = new OutboxPendingEvent(
                "ShipmentShipped:" + normalizedShipmentId,
                "SHIPMENT",
                normalizedShipmentId,
                "ShipmentShipped",
                buildShipmentShippedPayload(candidate));

        Shipment saved = shipmentWriteTx
                .saveStatusWithOutbox(candidate, current.getStatus(), outboxEvent)
                .orElseThrow(() -> new InvalidStatusTransitionException(current.getStatus(), ShipmentStatus.SHIPPED));

        log.info("Shipment {} -> SHIPPED atomically (orderId={})", normalizedShipmentId, saved.getOrderId());
        return saved;
    }

    @Override
    public Shipment markAsDelivered(String shipmentId) {
        String normalizedShipmentId = requireNonBlank(shipmentId, "shipmentId").trim();

        Shipment current = getById(normalizedShipmentId);

        if (current.getStatus() == ShipmentStatus.DELIVERED) {
            log.info("Shipment already DELIVERED (idempotent), returning existing shipmentId={}", normalizedShipmentId);
            return current;
        }

        Shipment updated = current.withStatus(ShipmentStatus.DELIVERED);

        OutboxPendingEvent outboxEvent = new OutboxPendingEvent(
                "ShipmentDelivered:" + normalizedShipmentId,
                "SHIPMENT",
                normalizedShipmentId,
                "ShipmentDelivered",
                buildShipmentDeliveredPayload(updated));

        Shipment saved = shipmentWriteTx
                .saveStatusWithOutbox(updated, current.getStatus(), outboxEvent)
                .orElseThrow(() -> new InvalidStatusTransitionException(current.getStatus(), ShipmentStatus.DELIVERED));

        log.info("Shipment {} -> DELIVERED atomically (orderId={})", normalizedShipmentId, saved.getOrderId());
        return saved;
    }

    @Override
    public String getShippingGuideUrl(String shipmentId) {
        String normalizedId = requireNonBlank(shipmentId, "shipmentId").trim();
        Shipment shipment = getById(normalizedId);
        if (shipment.getShippingGuideS3Key() == null) {
            throw new ShipmentGuideNotReadyException(normalizedId);
        }
        return shippingGuideStorage.getPresignedUrl(shipment.getShippingGuideS3Key(), Duration.ofMinutes(15));
    }

    private String buildShipmentDeliveredPayload(Shipment shipment) {
        try {
            return mapper.writeValueAsString(
                    new ShipmentDeliveredPayload(
                            shipment.getOrderId(),
                            shipment.getShipmentId()));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize ShipmentDelivered payload: " + e.getMessage(), e);
        }
    }

    private String buildShipmentShippedPayload(Shipment shipment) {
        try {
            return mapper.writeValueAsString(
                    new ShipmentShippedPayload(
                            shipment.getOrderId(),
                            shipment.getShipmentId(),
                            shipment.getTrackingId(),
                            shipment.getShippingGuideS3Key(),
                            shipment.getCarrier().name(),
                            shipment.getEstimatedDeliveryAt() != null
                                ? shipment.getEstimatedDeliveryAt().toString()
                                : null));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize ShipmentShipped payload: " + e.getMessage(), e);
        }
    }
}