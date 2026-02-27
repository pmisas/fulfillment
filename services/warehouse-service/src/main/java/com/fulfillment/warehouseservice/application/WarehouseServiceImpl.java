package com.fulfillment.warehouseservice.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.warehouseservice.application.dto.CreateWarehouseCommand;
import com.fulfillment.warehouseservice.application.dto.WarehouseOrderActionPayload;
import com.fulfillment.warehouseservice.application.dto.WarehouseStartFlowCommand;
import com.fulfillment.warehouseservice.domain.exception.WarehouseNotFoundException;
import com.fulfillment.warehouseservice.domain.model.Warehouse;
import com.fulfillment.warehouseservice.domain.port.OutboxEventsRepository;
import com.fulfillment.warehouseservice.domain.port.OutboxEventsRepository.OutboxPendingEvent;
import com.fulfillment.warehouseservice.domain.port.WarehouseRepository;

import static com.fulfillment.warehouseservice.domain.shared.DomainValidations.requireNonBlank;

@Service
public class WarehouseServiceImpl implements WarehouseService{

    private static final String AGGREGATE_TYPE = "WAREHOUSE";
    private static final String PICKING_STARTED_EVENT = "PickingStarted";
    private static final String PACKING_STARTED_EVENT = "PackingStarted";
    
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
    public Warehouse create(CreateWarehouseCommand command) {
        Warehouse warehouse = Warehouse.createWarehouse(
                                command.city(), 
                                command.lat(), 
                                command.lng()
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
    public boolean existsAny() {
        return this.warehouseRepo.existsAny();
    }

    @Override 
    public boolean existsById(String warehouseId) {
        return this.warehouseRepo.existsById(warehouseId);
    }

    @Override
    public void startPicking(WarehouseStartFlowCommand command) {
        publishWarehouseFlowEvent(command, PICKING_STARTED_EVENT);
    }

    @Override
    public void startPacking(WarehouseStartFlowCommand command) {
        publishWarehouseFlowEvent(command, PACKING_STARTED_EVENT);
    }

    private void publishWarehouseFlowEvent(WarehouseStartFlowCommand command, String eventType) {
        String warehouseId = requireNonBlank(command.warehouseId(), "warehouseId").trim();
        String orderId = requireNonBlank(command.orderId(), "orderId").trim();

        if (!warehouseRepo.existsById(warehouseId)) {
            throw new WarehouseNotFoundException(warehouseId);
        }

        OutboxPendingEvent pendingEvent = new OutboxPendingEvent(
                UUID.randomUUID().toString(),
                AGGREGATE_TYPE,
                warehouseId,
                eventType,
                buildWarehouseOrderActionPayload(orderId, warehouseId)
        );

        outboxRepo.savePending(pendingEvent);
    }

    private String buildWarehouseOrderActionPayload(String orderId, String warehouseId) {
        try {
            var payload = new WarehouseOrderActionPayload(orderId, warehouseId);
            return mapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize warehouse action payload: " + e.getMessage(), e);
        }
    }

}
