package com.fulfillment.shippingservice.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fulfillment.shippingservice.application.dto.CreateShipmentCommand;
import com.fulfillment.shippingservice.domain.model.CarrierCode;
import com.fulfillment.shippingservice.domain.model.Shipment;
import com.fulfillment.shippingservice.domain.model.ShipmentItem;
import com.fulfillment.shippingservice.domain.model.ShipmentStatus;
import com.fulfillment.shippingservice.domain.ports.ShipmentRepository;

@ExtendWith(MockitoExtension.class)
class ShippingServiceImplTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    private ShippingServiceImpl shippingService;

    @BeforeEach
    void setUp() {
        shippingService = new ShippingServiceImpl(shipmentRepository);
    }

    @Test
    void create_shouldPersistShipment() {
        CreateShipmentCommand command = new CreateShipmentCommand(
                "order-1",
                "wh-1",
                CarrierCode.INTERNAL_CARRIER,
                List.of(new CreateShipmentCommand.Item("SKU-1", 3)),
                Instant.now().plusSeconds(86400));

        when(shipmentRepository.save(org.mockito.ArgumentMatchers.any(Shipment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Shipment created = shippingService.create(command);

        assertNotNull(created.getShipmentId());
        assertEquals(ShipmentStatus.PENDING, created.getStatus());

        ArgumentCaptor<Shipment> captor = ArgumentCaptor.forClass(Shipment.class);
        verify(shipmentRepository).save(captor.capture());
        assertEquals("order-1", captor.getValue().getOrderId());
    }

    @Test
    void markAsShipped_shouldSetTrackingAndStatus() {
        Shipment pending = Shipment.createShipment(
                "ship-1",
                "order-1",
                "wh-1",
                CarrierCode.INTERNAL_CARRIER,
                List.of(ShipmentItem.createShipmentItem("SKU-1", 1)),
                Instant.now().plusSeconds(86400));

        when(shipmentRepository.findById("ship-1")).thenReturn(Optional.of(pending));
        when(shipmentRepository.save(org.mockito.ArgumentMatchers.any(Shipment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Shipment shipped = shippingService.markAsShipped("ship-1", "trk-1");

        assertEquals(ShipmentStatus.SHIPPED, shipped.getStatus());
        assertEquals("trk-1", shipped.getTrackingId());
        assertNotNull(shipped.getShippedAt());
    }
}
