package com.fulfillment.shippingservice.application;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.shippingservice.domain.exception.InvalidStatusTransitionException;
import com.fulfillment.shippingservice.domain.exception.ShipmentGuideNotReadyException;
import com.fulfillment.shippingservice.domain.exception.ShipmentNotFoundException;
import com.fulfillment.shippingservice.domain.model.CarrierCode;
import com.fulfillment.shippingservice.domain.model.Shipment;
import com.fulfillment.shippingservice.domain.model.ShipmentItem;
import com.fulfillment.shippingservice.domain.model.ShipmentStatus;
import com.fulfillment.shippingservice.domain.ports.OutboxEventsRepository.OutboxPendingEvent;
import com.fulfillment.shippingservice.domain.ports.ShipmentRepository;
import com.fulfillment.shippingservice.domain.ports.ShipmentWriteTransaction;
import com.fulfillment.shippingservice.domain.ports.ShippingGuidePdfGenerator;
import com.fulfillment.shippingservice.domain.ports.ShippingGuideStorage;

class ShippingServiceImplTest {

    private ShipmentRepository shipmentRepository;
    private ShipmentWriteTransaction shipmentWriteTx;
    private ShippingGuidePdfGenerator pdfGenerator;
    private ShippingGuideStorage shippingGuideStorage;

    private ShippingServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        shipmentRepository = mock(ShipmentRepository.class);
        shipmentWriteTx = mock(ShipmentWriteTransaction.class);
        pdfGenerator = mock(ShippingGuidePdfGenerator.class);
        shippingGuideStorage = mock(ShippingGuideStorage.class);

        service = new ShippingServiceImpl(
            shipmentRepository,
            shipmentWriteTx,
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

        when(shipmentRepository.findByOrderId("order-1")).thenReturn(List.of(existing));

        Shipment result = service.create(
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            List.of(new ShippingService.ShipmentItemInput("SKU-1", 2)),
            Instant.now().plusSeconds(86400)
        );

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

        when(shipmentRepository.findByOrderId("order-1")).thenReturn(List.of(existing));
        when(pdfGenerator.generate(existing)).thenReturn(new byte[]{1, 2, 3});
        when(shippingGuideStorage.upload(eq("ship-1"), any())).thenReturn("shipments/ship-1/guide.pdf");
        when(shipmentRepository.save(any())).thenReturn(withGuide);

        Shipment result = service.create(
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            List.of(new ShippingService.ShipmentItemInput("SKU-1", 2)),
            Instant.now().plusSeconds(86400)
        );

        assertEquals("shipments/ship-1/guide.pdf", result.getShippingGuideS3Key());
        verify(pdfGenerator).generate(existing);
        verify(shippingGuideStorage).upload(eq("ship-1"), any());
        verify(shipmentRepository).save(any());
    }

    @Test
    void create_shouldCreateShipmentAndUploadGuideWhenShipmentDoesNotExist() {
        when(shipmentRepository.findByOrderId("order-1")).thenReturn(List.of());
        when(pdfGenerator.generate(any())).thenReturn(new byte[]{1, 2});
        when(shippingGuideStorage.upload(anyString(), any())).thenReturn("shipments/generated/guide.pdf");
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Shipment result = service.create(
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            List.of(new ShippingService.ShipmentItemInput("SKU-1", 2)),
            Instant.now().plusSeconds(86400)
        );

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

        ShipmentNotFoundException exception =
            assertThrows(ShipmentNotFoundException.class, () -> service.getById("missing"));

        assertNotNull(exception);
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
        verify(shipmentWriteTx, never()).saveStatusWithOutbox(any(), any(), any());
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
        when(shipmentWriteTx.saveStatusWithOutbox(any(), eq(ShipmentStatus.PENDING), any()))
            .thenReturn(Optional.of(shipped));

        Shipment result = service.markAsShipped("ship-1");

        assertEquals(ShipmentStatus.SHIPPED, result.getStatus());

        ArgumentCaptor<OutboxPendingEvent> captor = ArgumentCaptor.forClass(OutboxPendingEvent.class);
        verify(shipmentWriteTx).saveStatusWithOutbox(any(), eq(ShipmentStatus.PENDING), captor.capture());

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
        when(shipmentWriteTx.saveStatusWithOutbox(any(), eq(ShipmentStatus.PENDING), any()))
            .thenReturn(Optional.empty());

        InvalidStatusTransitionException exception =
            assertThrows(InvalidStatusTransitionException.class, () -> service.markAsShipped("ship-1"));

        assertNotNull(exception);
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
        when(shipmentWriteTx.saveStatusWithOutbox(any(), eq(ShipmentStatus.SHIPPED), any()))
            .thenReturn(Optional.of(delivered));

        Shipment result = service.markAsDelivered("ship-1");

        assertEquals(ShipmentStatus.DELIVERED, result.getStatus());
    }

    @Test
    void markAsDelivered_shouldThrowWhenConditionalSaveFails() {
        Shipment shipped = Shipment.createShipment(
            "ship-1",
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            List.of(ShipmentItem.createShipmentItem("SKU-1", 2)),
            Instant.now().plusSeconds(86400)
        ).withStatus(ShipmentStatus.SHIPPED);

        when(shipmentRepository.findById("ship-1")).thenReturn(Optional.of(shipped));
        when(shipmentWriteTx.saveStatusWithOutbox(any(), eq(ShipmentStatus.SHIPPED), any()))
            .thenReturn(Optional.empty());

        InvalidStatusTransitionException exception =
            assertThrows(InvalidStatusTransitionException.class, () -> service.markAsDelivered("ship-1"));

        assertNotNull(exception);
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

        ShipmentGuideNotReadyException exception =
            assertThrows(ShipmentGuideNotReadyException.class, () -> service.getShippingGuideUrl("ship-1"));

        assertNotNull(exception);
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
