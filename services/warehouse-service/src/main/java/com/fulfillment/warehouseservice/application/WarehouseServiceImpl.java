package com.fulfillment.warehouseservice.application;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.warehouseservice.application.dto.WarehouseOrderActionPayload;
import com.fulfillment.warehouseservice.domain.exception.WarehouseNotFoundException;
import com.fulfillment.warehouseservice.domain.model.Warehouse;
import com.fulfillment.warehouseservice.domain.port.OutboxEventsRepository;
import com.fulfillment.warehouseservice.domain.port.OutboxEventsRepository.OutboxPendingEvent;
import com.fulfillment.warehouseservice.domain.port.WarehouseRepository;

import static com.fulfillment.warehouseservice.domain.shared.DomainValidations.requireNonBlank;

@Service
public class WarehouseServiceImpl implements WarehouseService{

    private static final Logger log = LoggerFactory.getLogger(WarehouseServiceImpl.class);

    private final WarehouseRepository warehouseRepo;
    private final OutboxEventsRepository outboxRepo;
    private final ObjectMapper mapper;

    public WarehouseServiceImpl(
        WarehouseRepository warehouseRepo, 
        OutboxEventsRepository outboxRepo,
        ObjectMapper mapper) {
        this.warehouseRepo = warehouseRepo;
        this.outboxRepo = outboxRepo;
        this.mapper = mapper;
    }

    @Override
    public Warehouse create(String city, double lat, double lng) {
        Warehouse warehouse = Warehouse.createWarehouse(
                                city, 
                                lat, 
                                lng
        );
        warehouseRepo.save(warehouse);
        return warehouse;
    }

    @Override
    public Warehouse getById(String warehouseId) {
        return this.warehouseRepo.findById(warehouseId)
        .orElseThrow(() -> new WarehouseNotFoundException(warehouseId));
    }

    @Override
    public List<Warehouse> getAll() {
        return this.warehouseRepo.findAll();
    }

    @Override 
    public boolean existsById(String warehouseId) {
        return this.warehouseRepo.existsById(warehouseId);
    }

    @Override
    public void completePicking(String warehouseId, String orderId) {
        publishWarehouseFlowEvent(warehouseId, orderId, "PickingCompleted");
    }

    @Override
    public void completePacking(String warehouseId, String orderId) {
        publishWarehouseFlowEvent(warehouseId, orderId, "PackingCompleted");
    }

    private void publishWarehouseFlowEvent(String warehouseId, String orderId, String eventType) {
        String normalizedWarehouseId = requireNonBlank(warehouseId, "warehouseId").trim();
        String normalizedOrderId = requireNonBlank(orderId, "orderId").trim();

        log.info("Publishing warehouse flow event: eventType={}, orderId={}, warehouseId={}", 
                 eventType, normalizedOrderId, normalizedWarehouseId);

        if (!warehouseRepo.existsById(normalizedWarehouseId)) {
            log.warn("Warehouse not found: warehouseId={}", normalizedWarehouseId);
            throw new WarehouseNotFoundException(normalizedWarehouseId);
        }
    
        String eventId = eventType + ":" + normalizedOrderId;

        OutboxPendingEvent pendingEvent = new OutboxPendingEvent(
            eventId,
            "ORDER",
            normalizedOrderId,
            eventType,
            buildWarehouseOrderActionPayload(normalizedOrderId, normalizedWarehouseId)
        );

        boolean saved = outboxRepo.savePendingIfAbsent(pendingEvent);
        
        if (saved) {
            log.info("Outbox event created successfully: eventId={}", eventId);
        } else {
            log.warn("Outbox event already exists, attempting to reset to PENDING: eventId={}", eventId);
            boolean reset = outboxRepo.resetToPendingIfProcessed(eventId);
            if (reset) {
                log.info("Outbox event reset to PENDING for re-processing: eventId={}", eventId);
            } else {
                log.info("Outbox event already PENDING or being processed: eventId={}", eventId);
            }
        }
    }

    private String buildWarehouseOrderActionPayload(String orderId, String warehouseId) {
        try {
            var payload = new WarehouseOrderActionPayload(orderId, warehouseId);
            return mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize warehouse action payload: " + e.getMessage(), e);
        }
    }

}
