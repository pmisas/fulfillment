package com.fulfillment.warehouseservice.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fulfillment.warehouseservice.application.dto.AssignWarehouseManagerCommand;
import com.fulfillment.warehouseservice.domain.exception.UserNotFoundException;
import com.fulfillment.warehouseservice.domain.exception.UserRoleNotAllowedException;
import com.fulfillment.warehouseservice.domain.exception.WarehouseAccessNotFoundException;
import com.fulfillment.warehouseservice.domain.exception.WarehouseManagerAssignmentConflictException;
import com.fulfillment.warehouseservice.domain.exception.WarehouseNotFoundException;
import com.fulfillment.warehouseservice.domain.model.WarehouseAccess;
import com.fulfillment.warehouseservice.domain.port.UserDirectory;
import com.fulfillment.warehouseservice.domain.port.WarehouseAccessRepository;
import com.fulfillment.warehouseservice.domain.port.WarehouseRepository;

class WarehouseAccessServiceImplTest {

    private WarehouseAccessRepository warehouseAccessRepository;
    private WarehouseRepository warehouseRepository;
    private UserDirectory userDirectory;
    private WarehouseAccessServiceImpl service;

    @BeforeEach
    void setUp() {
        warehouseAccessRepository = mock(WarehouseAccessRepository.class);
        warehouseRepository = mock(WarehouseRepository.class);
        userDirectory = mock(UserDirectory.class);
        service = new WarehouseAccessServiceImpl(warehouseAccessRepository, warehouseRepository, userDirectory);
    }

    @Test
    void assignManager_shouldSaveAssignmentWhenValidManager() {
        when(warehouseRepository.existsById("wh-1")).thenReturn(true);
        when(userDirectory.findById("user-1"))
            .thenReturn(Optional.of(new UserDirectory.DirectoryUser("user-1", "username-1", Set.of("WAREHOUSE_MANAGER"))));
        when(warehouseAccessRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseAccess result = service.assignManager(new AssignWarehouseManagerCommand("wh-1", "user-1", "admin-1"));

        assertEquals("user-1", result.getUserId());
        assertEquals("wh-1", result.getWarehouseId());
        assertTrue(result.isActive());
        assertEquals("admin-1", result.getAssignedBy());
        verify(warehouseAccessRepository).save(any());
    }

    @Test
    void assignManager_shouldRejectDuplicateActiveAssignmentOnSameWarehouse() {
        WarehouseAccess access = WarehouseAccess.restore("user-1", "wh-1", true, Instant.now(), "admin-1", Instant.now());

        when(warehouseRepository.existsById("wh-1")).thenReturn(true);
        when(userDirectory.findById("user-1"))
            .thenReturn(Optional.of(new UserDirectory.DirectoryUser("user-1", "username-1", Set.of("WAREHOUSE_MANAGER"))));
        when(warehouseAccessRepository.findByUserId("user-1")).thenReturn(Optional.of(access));

        assertThrows(WarehouseManagerAssignmentConflictException.class,
            () -> service.assignManager(new AssignWarehouseManagerCommand("wh-1", "user-1", "admin-1")));
    }

    @Test
    void assignManager_shouldReplacePreviousWarehouseWhenUserHasAnotherActiveAssignment() {
        WarehouseAccess previous = WarehouseAccess.restore("user-1", "wh-2", true, Instant.now(), "admin-old", Instant.now());

        when(warehouseRepository.existsById("wh-1")).thenReturn(true);
        when(userDirectory.findById("user-1"))
            .thenReturn(Optional.of(new UserDirectory.DirectoryUser("user-1", "username-1", Set.of("WAREHOUSE_MANAGER"))));
        when(warehouseAccessRepository.findByUserId("user-1")).thenReturn(Optional.of(previous));
        when(warehouseAccessRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseAccess result = service.assignManager(new AssignWarehouseManagerCommand("wh-1", "user-1", "admin-1"));

        assertEquals("wh-1", result.getWarehouseId());
        assertTrue(result.isActive());
    }

    @Test
    void assignManager_shouldFailWhenWarehouseDoesNotExist() {
        when(warehouseRepository.existsById("missing")).thenReturn(false);

        assertThrows(WarehouseNotFoundException.class,
            () -> service.assignManager(new AssignWarehouseManagerCommand("missing", "user-1", "admin-1")));
    }

    @Test
    void assignManager_shouldFailWhenUserDoesNotExist() {
        when(warehouseRepository.existsById("wh-1")).thenReturn(true);
        when(userDirectory.findById("user-1")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
            () -> service.assignManager(new AssignWarehouseManagerCommand("wh-1", "user-1", "admin-1")));
    }

    @Test
    void assignManager_shouldFailWhenUserIsNotWarehouseManager() {
        when(warehouseRepository.existsById("wh-1")).thenReturn(true);
        when(userDirectory.findById("user-1"))
            .thenReturn(Optional.of(new UserDirectory.DirectoryUser("user-1", "username-1", Set.of("OPERATOR"))));

        assertThrows(UserRoleNotAllowedException.class,
            () -> service.assignManager(new AssignWarehouseManagerCommand("wh-1", "user-1", "admin-1")));
    }

    @Test
    void removeManager_shouldDeactivateAssignment() {
        WarehouseAccess access = WarehouseAccess.restore("user-1", "wh-1", true, Instant.now(), "admin-1", Instant.now());

        when(warehouseRepository.existsById("wh-1")).thenReturn(true);
        when(warehouseAccessRepository.findByUserId("user-1")).thenReturn(Optional.of(access));
        when(warehouseAccessRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseAccess result = service.removeManager("wh-1", "user-1");

        assertFalse(result.isActive());
    }

    @Test
    void removeManager_shouldFailWhenAssignmentDoesNotExist() {
        when(warehouseRepository.existsById("wh-1")).thenReturn(true);
        when(warehouseAccessRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        assertThrows(WarehouseAccessNotFoundException.class, () -> service.removeManager("wh-1", "user-1"));
    }

    @Test
    void getManagersByWarehouse_shouldReturnActiveManagerIds() {
        when(warehouseRepository.existsById("wh-1")).thenReturn(true);
        when(warehouseAccessRepository.findActiveByWarehouseId("wh-1")).thenReturn(List.of(
            WarehouseAccess.restore("user-1", "wh-1", true, Instant.now(), "admin-1", Instant.now()),
            WarehouseAccess.restore("user-2", "wh-1", true, Instant.now(), "admin-1", Instant.now())
        ));

        List<String> result = service.getManagersByWarehouse("wh-1");

        assertEquals(List.of("user-1", "user-2"), result);
    }
}
