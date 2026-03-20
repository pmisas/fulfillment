package com.fulfillment.warehouseservice.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

import com.fulfillment.warehouseservice.domain.exception.WarehouseAccessDeniedException;
import com.fulfillment.warehouseservice.domain.model.WarehouseAccess;
import com.fulfillment.warehouseservice.domain.port.WarehouseAccessRepository;

class WarehouseAccessAuthorizationServiceTest {

    private WarehouseAccessRepository warehouseAccessRepository;
    private WarehouseAccessAuthorizationService service;

    @BeforeEach
    void setUp() {
        warehouseAccessRepository = mock(WarehouseAccessRepository.class);
        service = new WarehouseAccessAuthorizationService(warehouseAccessRepository);
    }

    @Test
    void assertCanAccessWarehouse_shouldAllowAdmin() {
        var auth = new TestingAuthenticationToken("admin-1", null, "ROLE_ADMIN");

        assertDoesNotThrow(() -> service.assertCanAccessWarehouse(auth, "wh-1"));
    }

    @Test
    void assertCanAccessWarehouse_shouldAllowAssignedManager() {
        var auth = new TestingAuthenticationToken("user-1", null, "ROLE_WAREHOUSE_MANAGER");

        when(warehouseAccessRepository.findByUserId("user-1"))
            .thenReturn(Optional.of(WarehouseAccess.restore("user-1", "wh-1", true, Instant.now(), "admin-1", Instant.now())));

        assertDoesNotThrow(() -> service.assertCanAccessWarehouse(auth, "wh-1"));
    }

    @Test
    void assertCanAccessWarehouse_shouldRejectUnassignedManager() {
        var auth = new TestingAuthenticationToken("user-1", null, "ROLE_WAREHOUSE_MANAGER");

        when(warehouseAccessRepository.findByUserId("user-1"))
            .thenReturn(Optional.of(WarehouseAccess.restore("user-1", "wh-2", true, Instant.now(), "admin-1", Instant.now())));

        assertThrows(WarehouseAccessDeniedException.class, () -> service.assertCanAccessWarehouse(auth, "wh-1"));
    }

    @Test
    void assertCanAccessWarehouse_shouldRejectNonManagerNonAdmin() {
        var auth = new TestingAuthenticationToken("user-1", null, "ROLE_OPERATOR");

        assertThrows(WarehouseAccessDeniedException.class, () -> service.assertCanAccessWarehouse(auth, "wh-1"));
    }
}
