package com.fulfillment.warehouseservice.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.warehouseservice.application.dto.CreateWarehouseCommand;
import com.fulfillment.warehouseservice.application.dto.WarehouseStartFlowCommand;
import com.fulfillment.warehouseservice.domain.exception.WarehouseNotFoundException;
import com.fulfillment.warehouseservice.domain.model.Warehouse;
import com.fulfillment.warehouseservice.domain.port.OutboxEventsRepository;
import com.fulfillment.warehouseservice.domain.port.OutboxEventsRepository.OutboxPendingEvent;
import com.fulfillment.warehouseservice.domain.port.WarehouseRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WarehouseServiceImplTest {

    private WarehouseRepository warehouseRepo;
    private OutboxEventsRepository outboxRepo;
    private WarehouseServiceImpl service;

    @BeforeEach
    void setUp() {
        warehouseRepo = mock(WarehouseRepository.class);
        outboxRepo = mock(OutboxEventsRepository.class);

        service = new WarehouseServiceImpl(
            warehouseRepo,
            outboxRepo,
            new ObjectMapper()
        );
    }

    @Test
    void create_shouldSaveAndReturnWarehouse() {
        CreateWarehouseCommand command = new CreateWarehouseCommand("Bogota", 4.7110, -74.0721);

        Warehouse result = service.create(command);

        assertNotNull(result);
        assertEquals("bogota", result.getCity());
        assertEquals(4.7110, result.getLat());
        assertEquals(-74.0721, result.getLng());

        verify(warehouseRepo).save(any(Warehouse.class));
    }

    @Test
    void getById_shouldReturnWarehouseWhenExists() {
        Warehouse warehouse = Warehouse.restore("wh-1", "bogota", 4.7110, -74.0721, java.time.Instant.now());

        when(warehouseRepo.findById("wh-1")).thenReturn(Optional.of(warehouse));

        Warehouse result = service.getById("wh-1");

        assertSame(warehouse, result);
    }

    @Test
    void getById_shouldThrowWhenWarehouseDoesNotExist() {
        when(warehouseRepo.findById("missing")).thenReturn(Optional.empty());

        assertThrows(WarehouseNotFoundException.class, () -> service.getById("missing"));
    }

    @Test
    void getAll_shouldReturnAllWarehouses() {
        List<Warehouse> warehouses = List.of(
            Warehouse.restore("wh-1", "bogota", 4.7110, -74.0721, java.time.Instant.now()),
            Warehouse.restore("wh-2", "medellin", 6.2442, -75.5812, java.time.Instant.now())
        );

        when(warehouseRepo.findAll()).thenReturn(warehouses);

        List<Warehouse> result = service.getAll();

        assertEquals(2, result.size());
        assertSame(warehouses, result);
    }

    @Test
    void existsById_shouldDelegateToRepository() {
        when(warehouseRepo.existsById("wh-1")).thenReturn(true);

        boolean result = service.existsById("wh-1");

        assertTrue(result);
        verify(warehouseRepo).existsById("wh-1");
    }

    @Test
    void completePicking_shouldPublishEventWhenWarehouseExists() {
        WarehouseStartFlowCommand command = new WarehouseStartFlowCommand("wh-1", "order-1");

        when(warehouseRepo.existsById("wh-1")).thenReturn(true);
        when(outboxRepo.savePendingIfAbsent(any())).thenReturn(true);

        service.completePicking(command);

        ArgumentCaptor<OutboxPendingEvent> captor = ArgumentCaptor.forClass(OutboxPendingEvent.class);
        verify(outboxRepo).savePendingIfAbsent(captor.capture());

        OutboxPendingEvent event = captor.getValue();
        assertEquals("PickingCompleted:order-1", event.eventId());
        assertEquals("ORDER", event.aggregateType());
        assertEquals("order-1", event.aggregateId());
        assertEquals("PickingCompleted", event.eventType());
        assertTrue(event.payload().contains("\"orderId\":\"order-1\""));
        assertTrue(event.payload().contains("\"warehouseId\":\"wh-1\""));

        verify(outboxRepo, never()).resetToPendingIfProcessed(anyString());
    }

    @Test
    void completePacking_shouldPublishEventWhenWarehouseExists() {
        WarehouseStartFlowCommand command = new WarehouseStartFlowCommand("wh-1", "order-1");

        when(warehouseRepo.existsById("wh-1")).thenReturn(true);
        when(outboxRepo.savePendingIfAbsent(any())).thenReturn(true);

        service.completePacking(command);

        ArgumentCaptor<OutboxPendingEvent> captor = ArgumentCaptor.forClass(OutboxPendingEvent.class);
        verify(outboxRepo).savePendingIfAbsent(captor.capture());

        OutboxPendingEvent event = captor.getValue();
        assertEquals("PackingCompleted:order-1", event.eventId());
        assertEquals("PackingCompleted", event.eventType());
        assertTrue(event.payload().contains("\"orderId\":\"order-1\""));
        assertTrue(event.payload().contains("\"warehouseId\":\"wh-1\""));
    }

    @Test
    void completePicking_shouldThrowWhenWarehouseDoesNotExist() {
        WarehouseStartFlowCommand command = new WarehouseStartFlowCommand("wh-404", "order-1");

        when(warehouseRepo.existsById("wh-404")).thenReturn(false);

        assertThrows(WarehouseNotFoundException.class, () -> service.completePicking(command));

        verify(outboxRepo, never()).savePendingIfAbsent(any());
    }

    @Test
    void completePacking_shouldThrowWhenWarehouseDoesNotExist() {
        WarehouseStartFlowCommand command = new WarehouseStartFlowCommand("wh-404", "order-1");

        when(warehouseRepo.existsById("wh-404")).thenReturn(false);

        assertThrows(WarehouseNotFoundException.class, () -> service.completePacking(command));

        verify(outboxRepo, never()).savePendingIfAbsent(any());
    }

    @Test
    void completePicking_shouldTryResetWhenEventAlreadyExists() {
        WarehouseStartFlowCommand command = new WarehouseStartFlowCommand("wh-1", "order-1");

        when(warehouseRepo.existsById("wh-1")).thenReturn(true);
        when(outboxRepo.savePendingIfAbsent(any())).thenReturn(false);
        when(outboxRepo.resetToPendingIfProcessed("PickingCompleted:order-1")).thenReturn(true);

        assertDoesNotThrow(() -> service.completePicking(command));

        verify(outboxRepo).savePendingIfAbsent(any());
        verify(outboxRepo).resetToPendingIfProcessed("PickingCompleted:order-1");
    }

    @Test
    void completePacking_shouldTryResetWhenEventAlreadyExists() {
        WarehouseStartFlowCommand command = new WarehouseStartFlowCommand("wh-1", "order-1");

        when(warehouseRepo.existsById("wh-1")).thenReturn(true);
        when(outboxRepo.savePendingIfAbsent(any())).thenReturn(false);
        when(outboxRepo.resetToPendingIfProcessed("PackingCompleted:order-1")).thenReturn(true);

        assertDoesNotThrow(() -> service.completePacking(command));

        verify(outboxRepo).savePendingIfAbsent(any());
        verify(outboxRepo).resetToPendingIfProcessed("PackingCompleted:order-1");
    }
}
