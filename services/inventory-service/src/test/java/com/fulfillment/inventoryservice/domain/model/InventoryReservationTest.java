package com.fulfillment.inventoryservice.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class InventoryReservationTest {

    @Test
    void createInventoryReservation_shouldCreateWhenDataIsValid() {
        InventoryReservation reservation = InventoryReservation.createInventoryReservation(
            "resv-1",
            "order-1",
            "wh-1",
            List.of(new InventoryReservation.Item("SKU-1", 2))
        );

        assertEquals("resv-1", reservation.getReservationId());
        assertEquals("order-1", reservation.getOrderId());
        assertEquals("wh-1", reservation.getWarehouseId());
        assertEquals(1, reservation.getItems().size());
        assertNotNull(reservation.getCreatedAt());
    }

    @Test
    void createInventoryReservation_shouldThrowWhenReservationIdIsBlank() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> InventoryReservation.createInventoryReservation(
                " ",
                "order-1",
                "wh-1",
                List.of(new InventoryReservation.Item("SKU-1", 2))
            )
        );

        assertEquals("reservationId must be not blank", ex.getMessage());
    }

    @Test
    void createInventoryReservation_shouldThrowWhenOrderIdIsBlank() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> InventoryReservation.createInventoryReservation(
                "resv-1",
                " ",
                "wh-1",
                List.of(new InventoryReservation.Item("SKU-1", 2))
            )
        );

        assertEquals("orderId must be not blank", ex.getMessage());
    }

    @Test
    void createInventoryReservation_shouldThrowWhenWarehouseIdIsBlank() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> InventoryReservation.createInventoryReservation(
                "resv-1",
                "order-1",
                " ",
                List.of(new InventoryReservation.Item("SKU-1", 2))
            )
        );

        assertEquals("warehouseId must be not blank", ex.getMessage());
    }

    @Test
    void createInventoryReservation_shouldThrowWhenItemsAreEmpty() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> InventoryReservation.createInventoryReservation(
                "resv-1",
                "order-1",
                "wh-1",
                List.of()
            )
        );

        assertEquals("items cant be empty", ex.getMessage());
    }
}
