package com.fulfillment.shippingservice.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

import com.fulfillment.shippingservice.domain.exception.WarehouseAccessDeniedException;
import com.fulfillment.shippingservice.domain.model.CarrierCode;
import com.fulfillment.shippingservice.domain.model.Shipment;
import com.fulfillment.shippingservice.domain.model.ShipmentItem;
import com.fulfillment.shippingservice.domain.model.WarehouseAccess;
import com.fulfillment.shippingservice.domain.ports.WarehouseAccessRepository;

class ShippingWarehouseAuthorizationServiceTest {

    private WarehouseAccessRepository warehouseAccessRepository;
    private ShippingWarehouseAuthorizationService service;

    @BeforeEach
    void setUp() {
        warehouseAccessRepository = mock(WarehouseAccessRepository.class);
        service = new ShippingWarehouseAuthorizationService(warehouseAccessRepository);
    }

    @Test
    void assertCanAccessShipment_shouldAllowAdmin() {
        var auth = new TestingAuthenticationToken("admin-1", null, "ROLE_ADMIN");

        assertDoesNotThrow(() -> service.assertCanAccessShipment(auth, shipment("wh-1")));
    }

    @Test
    void assertCanAccessShipment_shouldAllowAssignedManager() {
        var auth = new TestingAuthenticationToken("manager-1", null, "ROLE_WAREHOUSE_MANAGER");
        when(warehouseAccessRepository.findByUserId("manager-1"))
            .thenReturn(Optional.of(WarehouseAccess.restore("manager-1", "wh-1", true)));

        assertDoesNotThrow(() -> service.assertCanAccessShipment(auth, shipment("wh-1")));
    }

    @Test
    void assertCanAccessShipment_shouldRejectManagerFromOtherWarehouse() {
        var auth = new TestingAuthenticationToken("manager-1", null, "ROLE_WAREHOUSE_MANAGER");
        when(warehouseAccessRepository.findByUserId("manager-1"))
            .thenReturn(Optional.of(WarehouseAccess.restore("manager-1", "wh-2", true)));

        assertThrows(WarehouseAccessDeniedException.class,
            () -> service.assertCanAccessShipment(auth, shipment("wh-1")));
    }

    @Test
    void filterAuthorizedShipments_shouldKeepOnlyAssignedWarehouseForManager() {
        var auth = new TestingAuthenticationToken("manager-1", null, "ROLE_WAREHOUSE_MANAGER");
        when(warehouseAccessRepository.findByUserId("manager-1"))
            .thenReturn(Optional.of(WarehouseAccess.restore("manager-1", "wh-1", true)));

        List<Shipment> filtered = service.filterAuthorizedShipments(auth, List.of(
            shipment("wh-1"),
            shipment("wh-2")
        ));

        assertEquals(1, filtered.size());
        assertEquals("wh-1", filtered.get(0).getWarehouseId());
    }

    private Shipment shipment(String warehouseId) {
        return Shipment.createShipment(
            "ship-" + warehouseId,
            "order-" + warehouseId,
            warehouseId,
            CarrierCode.INTERNAL_CARRIER,
            List.of(ShipmentItem.createShipmentItem("SKU-1", 1)),
            Instant.now().plusSeconds(3600)
        );
    }
}
