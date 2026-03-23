package com.fulfillment.inventoryservice.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

import com.fulfillment.inventoryservice.domain.exception.WarehouseAccessDeniedException;
import com.fulfillment.inventoryservice.domain.model.WarehouseAccess;
import com.fulfillment.inventoryservice.domain.ports.WarehouseAccessRepository;

class InventoryWarehouseAuthorizationServiceTest {

    private WarehouseAccessRepository warehouseAccessRepository;
    private InventoryWarehouseAuthorizationService service;

    @BeforeEach
    void setUp() {
        warehouseAccessRepository = mock(WarehouseAccessRepository.class);
        service = new InventoryWarehouseAuthorizationService(warehouseAccessRepository);
    }

    @Test
    void assertCanAccessWarehouse_shouldAllowAdmin() {
        var auth = new TestingAuthenticationToken("admin-1", null, "ROLE_ADMIN");

        assertDoesNotThrow(() -> service.assertCanAccessWarehouse(auth, "wh-1"));
    }

    @Test
    void assertCanAccessWarehouse_shouldAllowAssignedManager() {
        var auth = new TestingAuthenticationToken("manager-1", null, "ROLE_WAREHOUSE_MANAGER");
        when(warehouseAccessRepository.findByUserId("manager-1"))
            .thenReturn(Optional.of(WarehouseAccess.restore("manager-1", "wh-1", true)));

        assertDoesNotThrow(() -> service.assertCanAccessWarehouse(auth, "wh-1"));
    }

    @Test
    void assertCanAccessWarehouse_shouldRejectManagerFromOtherWarehouse() {
        var auth = new TestingAuthenticationToken("manager-1", null, "ROLE_WAREHOUSE_MANAGER");
        when(warehouseAccessRepository.findByUserId("manager-1"))
            .thenReturn(Optional.of(WarehouseAccess.restore("manager-1", "wh-2", true)));

        assertThrows(WarehouseAccessDeniedException.class,
            () -> service.assertCanAccessWarehouse(auth, "wh-1"));
    }

    @Test
    void assertCanAccessWarehouse_shouldRejectNonAdminNonManager() {
        var auth = new TestingAuthenticationToken("operator-1", null, "ROLE_OPERATOR");

        assertThrows(WarehouseAccessDeniedException.class,
            () -> service.assertCanAccessWarehouse(auth, "wh-1"));
    }
}
