package com.fulfillment.warehouseservice.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class WarehouseTest {

    @Test
    void createWarehouse_shouldCreateWarehouseWhenDataIsValid() {
        Warehouse warehouse = Warehouse.createWarehouse("Bogota", 4.7110, -74.0721);

        assertNotNull(warehouse.getWarehouseId());
        assertEquals("bogota", warehouse.getCity());
        assertEquals(4.7110, warehouse.getLat());
        assertEquals(-74.0721, warehouse.getLng());
        assertNotNull(warehouse.getCreatedAt());
    }

    @Test
    void createWarehouse_shouldThrowWhenCityIsNull() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> Warehouse.createWarehouse(null, 4.7110, -74.0721)
        );

        assertEquals("city must be not blank", ex.getMessage());
    }

    @Test
    void createWarehouse_shouldThrowWhenCityIsBlank() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> Warehouse.createWarehouse("   ", 4.7110, -74.0721)
        );

        assertEquals("city must be not blank", ex.getMessage());
    }

    @Test
    void createWarehouse_shouldThrowWhenLatIsTooLow() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> Warehouse.createWarehouse("Bogota", -91, -74.0721)
        );

        assertEquals("Latitude must be between -90 and 90", ex.getMessage());
    }

    @Test
    void createWarehouse_shouldThrowWhenLatIsTooHigh() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> Warehouse.createWarehouse("Bogota", 91, -74.0721)
        );

        assertEquals("Latitude must be between -90 and 90", ex.getMessage());
    }

    @Test
    void createWarehouse_shouldThrowWhenLngIsTooLow() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> Warehouse.createWarehouse("Bogota", 4.7110, -181)
        );

        assertEquals("Longitude must be between -180 and 180", ex.getMessage());
    }

    @Test
    void createWarehouse_shouldThrowWhenLngIsTooHigh() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> Warehouse.createWarehouse("Bogota", 4.7110, 181)
        );

        assertEquals("Longitude must be between -180 and 180", ex.getMessage());
    }

    @Test
    void restore_shouldRestoreWarehouseCorrectly() {
        Instant now = Instant.now();

        Warehouse warehouse = Warehouse.restore(
            "wh-1",
            "medellin",
            6.2442,
            -75.5812,
            now
        );

        assertEquals("wh-1", warehouse.getWarehouseId());
        assertEquals("medellin", warehouse.getCity());
        assertEquals(6.2442, warehouse.getLat());
        assertEquals(-75.5812, warehouse.getLng());
        assertEquals(now, warehouse.getCreatedAt());
    }
}
