package com.fulfillment.inventoryservice.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fulfillment.inventoryservice.application.dto.AvailabilityQuery;
import com.fulfillment.inventoryservice.application.dto.AvailabilityResult;
import com.fulfillment.inventoryservice.application.dto.ConsumeReservationCommand;
import com.fulfillment.inventoryservice.application.dto.ReserveBatchCommand;
import com.fulfillment.inventoryservice.application.dto.RestockBatchCommand;
import com.fulfillment.inventoryservice.application.dto.SkuQuantity;
import com.fulfillment.inventoryservice.domain.exception.WarehouseNotFoundException;
import com.fulfillment.inventoryservice.domain.model.InventoryItem;
import com.fulfillment.inventoryservice.domain.ports.InventoryItemsRepository;
import com.fulfillment.inventoryservice.domain.ports.InventoryReservationTransaction;
import com.fulfillment.inventoryservice.domain.ports.InventoryReservationTransaction.ConsumeResult;
import com.fulfillment.inventoryservice.domain.ports.InventoryReservationTransaction.ReserveResult;
import com.fulfillment.inventoryservice.domain.ports.InventoryRestockTransaction;
import com.fulfillment.inventoryservice.domain.ports.WarehouseClient;

class InventoryItemsServiceImplTest {

    private InventoryItemsRepository repo;
    private InventoryReservationTransaction reservationTx;
    private InventoryRestockTransaction restockTx;
    private WarehouseClient warehouseClient;

    private InventoryItemsServiceImpl service;

    @BeforeEach
    void setUp() {
        repo = mock(InventoryItemsRepository.class);
        reservationTx = mock(InventoryReservationTransaction.class);
        restockTx = mock(InventoryRestockTransaction.class);
        warehouseClient = mock(WarehouseClient.class);

        service = new InventoryItemsServiceImpl(repo, reservationTx, restockTx, warehouseClient);
    }

    @Test
    void consumeReservation_shouldDelegateToTransaction() {
        when(reservationTx.consumeAtomically("resv-1")).thenReturn(ConsumeResult.CONSUMED);

        ConsumeResult result = service.consumeReservation(new ConsumeReservationCommand("resv-1"));

        assertEquals(ConsumeResult.CONSUMED, result);
        verify(reservationTx).consumeAtomically("resv-1");
    }

    @Test
    void restockBatch_shouldThrowWhenWarehouseDoesNotExist() {
        RestockBatchCommand command = new RestockBatchCommand(
            "wh-404",
            List.of(new SkuQuantity("SKU-1", 5))
        );

        when(warehouseClient.existsById("wh-404")).thenReturn(false);

        assertThrows(WarehouseNotFoundException.class, () -> service.restockBatch(command));

        verify(restockTx, never()).restockAtomically(anyString(), anyList());
    }

    @Test
    void restockBatch_shouldAggregateQuantitiesBySku() {
        RestockBatchCommand command = new RestockBatchCommand(
            "wh-1",
            List.of(
                new SkuQuantity("SKU-1", 5),
                new SkuQuantity("SKU-1", 3),
                new SkuQuantity("SKU-2", 2)
            )
        );

        List<InventoryItem> stored = List.of(
            InventoryItem.restore("wh-1", "SKU-1", 8, 0, Instant.now()),
            InventoryItem.restore("wh-1", "SKU-2", 2, 0, Instant.now())
        );

        when(warehouseClient.existsById("wh-1")).thenReturn(true);
        when(repo.findByWarehouseId("wh-1")).thenReturn(stored);

        List<InventoryItem> result = service.restockBatch(command);

        assertEquals(2, result.size());
        verify(restockTx).restockAtomically(eq("wh-1"), anyList());
        verify(repo).findByWarehouseId("wh-1");
    }

    @Test
    void lowStock_shouldDelegateToRepository() {
        List<InventoryItem> items = List.of(
            InventoryItem.restore("wh-1", "SKU-1", 5, 4, Instant.now())
        );

        when(repo.findLowStock(2)).thenReturn(items);

        List<InventoryItem> result = service.lowStock(2);

        assertSame(items, result);
    }

    @Test
    void getByWarehouseId_shouldDelegateToRepository() {
        List<InventoryItem> items = List.of(
            InventoryItem.restore("wh-1", "SKU-1", 10, 0, Instant.now())
        );

        when(repo.findByWarehouseId("wh-1")).thenReturn(items);

        List<InventoryItem> result = service.getByWarehouseId("wh-1");

        assertSame(items, result);
    }

    @Test
    void checkAvailability_shouldReturnCanFulfillAllTrueWhenAllItemsHaveStock() {
        AvailabilityQuery query = new AvailabilityQuery(
            "wh-1",
            List.of(
                new SkuQuantity("SKU-1", 2),
                new SkuQuantity("SKU-2", 1)
            )
        );

        when(repo.findBySkus("wh-1", List.of("SKU-1", "SKU-2"))).thenReturn(List.of(
            InventoryItem.restore("wh-1", "SKU-1", 10, 3, Instant.now()), // available 7
            InventoryItem.restore("wh-1", "SKU-2", 5, 1, Instant.now())   // available 4
        ));

        AvailabilityResult result = service.checkAvailability(query);

        assertTrue(result.canFulfillAll());
        assertEquals(2, result.items().size());
    }

    @Test
    void checkAvailability_shouldReturnCanFulfillAllFalseWhenSomeItemHasNoStock() {
        AvailabilityQuery query = new AvailabilityQuery(
            "wh-1",
            List.of(
                new SkuQuantity("SKU-1", 2),
                new SkuQuantity("SKU-2", 3)
            )
        );

        when(repo.findBySkus("wh-1", List.of("SKU-1", "SKU-2"))).thenReturn(List.of(
            InventoryItem.restore("wh-1", "SKU-1", 10, 0, Instant.now())
        ));

        AvailabilityResult result = service.checkAvailability(query);

        assertFalse(result.canFulfillAll());
        assertEquals(2, result.items().size());

        AvailabilityResult.ItemAvailability missing = result.items().stream()
            .filter(i -> i.sku().equals("SKU-2"))
            .findFirst()
            .orElseThrow();

        assertEquals(0, missing.available());
        assertFalse(missing.canFulfill());
    }

    @Test
    void reserveItems_shouldBuildReservationAndDelegateToTransaction() {
        ReserveBatchCommand command = new ReserveBatchCommand(
            "resv-1",
            "order-1",
            "wh-1",
            List.of(
                new SkuQuantity("SKU-1", 2),
                new SkuQuantity("SKU-2", 1)
            )
        );

        when(reservationTx.reserveAtomically(any())).thenReturn(ReserveResult.RESERVED);

        ReserveResult result = service.reserveItems(command);

        assertEquals(ReserveResult.RESERVED, result);
        verify(reservationTx).reserveAtomically(any());
    }

    @Test
    void releaseReservation_shouldDelegateToTransaction() {
        service.releaseReservation("resv-1");

        verify(reservationTx).releaseAtomically("resv-1");
    }
}
