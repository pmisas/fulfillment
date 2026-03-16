package com.fulfillment.shippingservice.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.shippingservice.application.dto.CreateShipmentCommand;
import com.fulfillment.shippingservice.domain.exception.InvalidStatusTransitionException;
import com.fulfillment.shippingservice.domain.exception.ShipmentGuideNotReadyException;
import com.fulfillment.shippingservice.domain.exception.ShipmentNotFoundException;
import com.fulfillment.shippingservice.domain.model.CarrierCode;
import com.fulfillment.shippingservice.domain.model.Shipment;
import com.fulfillment.shippingservice.domain.model.ShipmentItem;
import com.fulfillment.shippingservice.domain.model.ShipmentStatus;
import com.fulfillment.shippingservice.domain.ports.OutboxEventsRepository;
import com.fulfillment.shippingservice.domain.ports.OutboxEventsRepository.OutboxPendingEvent;
import com.fulfillment.shippingservice.domain.ports.ShipmentRepository;
import com.fulfillment.shippingservice.domain.ports.ShippingGuidePdfGenerator;
import com.fulfillment.shippingservice.domain.ports.ShippingGuideStorage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ShippingServiceImplTest {

    private ShipmentRepository shipmentRepository;
    private OutboxEventsRepository outboxRepo;
    private ShippingGuidePdfGenerator pdfGenerator;
    private ShippingGuideStorage shippingGuideStorage;

    private ShippingServiceImpl service;

    @BeforeEach
    void setUp() {
        shipmentRepository = mock(ShipmentRepository.class);
        outboxRepo = mock(OutboxEventsRepository.class);
        pdfGenerator = mock(ShippingGuidePdfGenerator.class);
        shippingGuideStorage = mock(ShippingGuideStorage.class);

        service = new ShippingServiceImpl(
            shipmentRepository,
            outboxRepo,
            new ObjectMapper(),
            pdfGenerator,
            shippingGuideStorage
        );
    }

    @Test
    void create_shouldReturnExistingShipmentWhenAlreadyExistsWithGuide() {
        Shipment existing = Shipment.restore(
            "ship-1",
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            ShipmentStatus.PENDING,
            null,
            List.of(ShipmentItem.createShipmentItem("SKU-1", 2)),
            Instant.now(),
            null,
            Instant.now().plusSeconds(86400),
            "shipments/ship-1/guide.pdf"
        );

        CreateShipmentCommand command = new CreateShipmentCommand(
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            List.of(new CreateShipmentCommand.Item("SKU-1", 2)),
            Instant.now().plusSeconds(86400)
        );

        when(shipmentRepository.findByOrderId("order-1")).thenReturn(List.of(existing));

        Shipment result = service.create(command);

        assertSame(existing, result);
        verify(pdfGenerator, never()).generate(any());
        verify(shippingGuideStorage, never()).upload(anyString(), any());
    }

    @Test
    void create_shouldRegenerateGuideWhenShipmentExistsWithoutGuide() {
        Shipment existing = Shipment.restore(
            "ship-1",
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            ShipmentStatus.PENDING,
            null,
            List.of(ShipmentItem.createShipmentItem("SKU-1", 2)),
            Instant.now(),
            null,
            Instant.now().plusSeconds(86400),
            null
        );

        Shipment withGuide = existing.withShippingGuideS3Key("shipments/ship-1/guide.pdf");

        CreateShipmentCommand command = new CreateShipmentCommand(
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            List.of(new CreateShipmentCommand.Item("SKU-1", 2)),
            Instant.now().plusSeconds(86400)
        );

        when(shipmentRepository.findByOrderId("order-1")).thenReturn(List.of(existing));
        when(pdfGenerator.generate(existing)).thenReturn(new byte[]{1, 2, 3});
        when(shippingGuideStorage.upload(eq("ship-1"), any())).thenReturn("shipments/ship-1/guide.pdf");
        when(shipmentRepository.save(any())).thenReturn(withGuide);

        Shipment result = service.create(command);

        assertEquals("shipments/ship-1/guide.pdf", result.getShippingGuideS3Key());
        verify(pdfGenerator).generate(existing);
        verify(shippingGuideStorage).upload(eq("ship-1"), any());
        verify(shipmentRepository).save(any());
    }

    @Test
    void create_shouldCreateShipmentAndUploadGuideWhenShipmentDoesNotExist() {
        CreateShipmentCommand command = new CreateShipmentCommand(
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            List.of(new CreateShipmentCommand.Item("SKU-1", 2)),
            Instant.now().plusSeconds(86400)
        );

        when(shipmentRepository.findByOrderId("order-1")).thenReturn(List.of());
        when(pdfGenerator.generate(any())).thenReturn(new byte[]{1, 2});
        when(shippingGuideStorage.upload(anyString(), any())).thenReturn("shipments/generated/guide.pdf");
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Shipment result = service.create(command);

        assertNotNull(result.getShipmentId());
        assertEquals("order-1", result.getOrderId());
        assertEquals("wh-1", result.getWarehouseId());
        assertEquals("shipments/generated/guide.pdf", result.getShippingGuideS3Key());

        verify(shipmentRepository, times(2)).save(any());
        verify(pdfGenerator).generate(any());
        verify(shippingGuideStorage).upload(anyString(), any());
    }

    @Test
    void getById_shouldReturnShipmentWhenExists() {
        Shipment shipment = Shipment.createShipment(
            "ship-1",
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            List.of(ShipmentItem.createShipmentItem("SKU-1", 2)),
            Instant.now().plusSeconds(86400)
        );

        when(shipmentRepository.findById("ship-1")).thenReturn(Optional.of(shipment));

        Shipment result = service.getById("ship-1");

        assertSame(shipment, result);
    }

    @Test
    void getById_shouldThrowWhenShipmentDoesNotExist() {
        when(shipmentRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ShipmentNotFoundException.class, () -> service.getById("missing"));
    }

    @Test
    void getAll_shouldDelegateToRepository() {
        List<Shipment> shipments = List.of(
            Shipment.createShipment(
                "ship-1",
                "order-1",
                "wh-1",
                CarrierCode.INTERNAL_CARRIER,
                List.of(ShipmentItem.createShipmentItem("SKU-1", 2)),
                Instant.now().plusSeconds(86400)
            )
        );

        when(shipmentRepository.findAll()).thenReturn(shipments);

        List<Shipment> result = service.getAll();

        assertSame(shipments, result);
    }

    @Test
    void getByOrderId_shouldDelegateToRepository() {
        List<Shipment> shipments = List.of(
            Shipment.createShipment(
                "ship-1",
                "order-1",
                "wh-1",
                CarrierCode.INTERNAL_CARRIER,
                List.of(ShipmentItem.createShipmentItem("SKU-1", 2)),
                Instant.now().plusSeconds(86400)
            )
        );

        when(shipmentRepository.findByOrderId("order-1")).thenReturn(shipments);

        List<Shipment> result = service.getByOrderId("order-1");

        assertSame(shipments, result);
    }

    @Test
    void markAsShipped_shouldReturnSameShipmentWhenAlreadyShipped() {
        Shipment shipped = Shipment.createShipment(
            "ship-1",
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            List.of(ShipmentItem.createShipmentItem("SKU-1", 2)),
            Instant.now().plusSeconds(86400)
        ).withStatus(ShipmentStatus.SHIPPED);

        when(shipmentRepository.findById("ship-1")).thenReturn(Optional.of(shipped));

        Shipment result = service.markAsShipped("ship-1");

        assertSame(shipped, result);
        verify(outboxRepo, never()).savePendingIfAbsent(any());
    }

    @Test
    void markAsShipped_shouldUpdateStatusAndQueueOutbox() {
        Shipment pending = Shipment.createShipment(
            "ship-1",
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            List.of(ShipmentItem.createShipmentItem("SKU-1", 2)),
            Instant.now().plusSeconds(86400)
        ).withShippingGuideS3Key("shipments/ship-1/guide.pdf");

        Shipment shipped = pending.withStatus(ShipmentStatus.SHIPPED);

        when(shipmentRepository.findById("ship-1")).thenReturn(Optional.of(pending));
        when(shipmentRepository.saveIfStatusMatches(any(), eq(ShipmentStatus.PENDING)))
            .thenReturn(Optional.of(shipped));
        when(outboxRepo.savePendingIfAbsent(any())).thenReturn(true);

        Shipment result = service.markAsShipped("ship-1");

        assertEquals(ShipmentStatus.SHIPPED, result.getStatus());

        ArgumentCaptor<OutboxPendingEvent> captor = ArgumentCaptor.forClass(OutboxPendingEvent.class);
        verify(outboxRepo).savePendingIfAbsent(captor.capture());

        OutboxPendingEvent event = captor.getValue();
        assertEquals("ShipmentShipped", event.eventType());
        assertEquals("SHIPMENT", event.aggregateType());
        assertEquals("ship-1", event.aggregateId());
        assertTrue(event.eventId().startsWith("ShipmentShipped:ship-1"));
    }

    @Test
    void markAsShipped_shouldThrowWhenConditionalSaveFails() {
        Shipment pending = Shipment.createShipment(
            "ship-1",
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            List.of(ShipmentItem.createShipmentItem("SKU-1", 2)),
            Instant.now().plusSeconds(86400)
        );

        when(shipmentRepository.findById("ship-1")).thenReturn(Optional.of(pending));
        when(shipmentRepository.saveIfStatusMatches(any(), eq(ShipmentStatus.PENDING)))
            .thenReturn(Optional.empty());

        assertThrows(InvalidStatusTransitionException.class, () -> service.markAsShipped("ship-1"));
    }

    @Test
    void markAsDelivered_shouldReturnSameShipmentWhenAlreadyDelivered() {
        Shipment delivered = Shipment.createShipment(
            "ship-1",
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            List.of(ShipmentItem.createShipmentItem("SKU-1", 2)),
            Instant.now().plusSeconds(86400)
        ).withStatus(ShipmentStatus.SHIPPED)
         .withStatus(ShipmentStatus.DELIVERED);

        when(shipmentRepository.findById("ship-1")).thenReturn(Optional.of(delivered));

        Shipment result = service.markAsDelivered("ship-1");

        assertSame(delivered, result);
    }

    @Test
    void markAsDelivered_shouldUpdateStatus() {
        Shipment shipped = Shipment.createShipment(
            "ship-1",
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            List.of(ShipmentItem.createShipmentItem("SKU-1", 2)),
            Instant.now().plusSeconds(86400)
        ).withStatus(ShipmentStatus.SHIPPED);

        Shipment delivered = shipped.withStatus(ShipmentStatus.DELIVERED);

        when(shipmentRepository.findById("ship-1")).thenReturn(Optional.of(shipped));
        when(shipmentRepository.saveIfStatusMatches(any(), eq(ShipmentStatus.SHIPPED)))
            .thenReturn(Optional.of(delivered));

        Shipment result = service.markAsDelivered("ship-1");

        assertEquals(ShipmentStatus.DELIVERED, result.getStatus());
    }

    @Test
    void getShippingGuideUrl_shouldThrowWhenGuideIsNotReady() {
        Shipment shipment = Shipment.createShipment(
            "ship-1",
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            List.of(ShipmentItem.createShipmentItem("SKU-1", 2)),
            Instant.now().plusSeconds(86400)
        );

        when(shipmentRepository.findById("ship-1")).thenReturn(Optional.of(shipment));

        assertThrows(ShipmentGuideNotReadyException.class, () -> service.getShippingGuideUrl("ship-1"));
    }

    @Test
    void getShippingGuideUrl_shouldReturnPresignedUrlWhenGuideExists() {
        Shipment shipment = Shipment.createShipment(
            "ship-1",
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            List.of(ShipmentItem.createShipmentItem("SKU-1", 2)),
            Instant.now().plusSeconds(86400)
        ).withShippingGuideS3Key("shipments/ship-1/guide.pdf");

        when(shipmentRepository.findById("ship-1")).thenReturn(Optional.of(shipment));
        when(shippingGuideStorage.getPresignedUrl("shipments/ship-1/guide.pdf", Duration.ofMinutes(15)))
            .thenReturn("https://signed-url");

        String result = service.getShippingGuideUrl("ship-1");

        assertEquals("https://signed-url", result);
    }
}
